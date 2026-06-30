package mx.juarezdeoriente.solicitudes.requests.domain.model;

import mx.juarezdeoriente.solicitudes.requests.domain.event.RequestCancelledEvent;
import mx.juarezdeoriente.solicitudes.requests.domain.event.RequestIssuedEvent;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.domain.model.AggregateRoot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Request extends AggregateRoot {

    private final UUID id;
    private Long folio;
    private RequestStatus status;
    private UUID supplierId;
    private UUID solicitanteId;
    private String destination;
    private String authorizer;
    private UUID createdBy;
    private final List<RequestItem> items;
    private final Instant createdAt;
    private Instant issuedAt;
    private Instant cancelledAt;
    private String cancellationReason;

    private static final int MAX_ITEMS = 8;

    private Request(UUID id, Long folio, RequestStatus status, UUID supplierId,
                    UUID solicitanteId, String destination, String authorizer, UUID createdBy,
                    List<RequestItem> items, Instant createdAt,
                    Instant issuedAt, Instant cancelledAt, String cancellationReason) {
        this.id                 = id;
        this.folio              = folio;
        this.status             = status;
        this.supplierId         = supplierId;
        this.solicitanteId      = solicitanteId;
        this.destination        = destination;
        this.authorizer         = authorizer;
        this.createdBy          = createdBy;
        this.items              = new ArrayList<>(items);
        this.createdAt          = createdAt;
        this.issuedAt           = issuedAt;
        this.cancelledAt        = cancelledAt;
        this.cancellationReason = cancellationReason;
    }

    public static Request reconstitute(UUID id, Long folio, RequestStatus status,
                                       UUID supplierId, UUID solicitanteId,
                                       String destination, String authorizer,
                                       UUID createdBy, List<RequestItem> items, Instant createdAt,
                                       Instant issuedAt, Instant cancelledAt, String cancellationReason) {
        return new Request(id, folio, status, supplierId, solicitanteId, destination, authorizer,
                createdBy, items, createdAt, issuedAt, cancelledAt, cancellationReason);
    }

    /** Crea un borrador; el folio se asigna al emitir, no aquí. */
    public static Request createDraft(UUID supplierId, UUID solicitanteId,
                                      String destination, String authorizer, UUID createdBy) {
        if (supplierId == null) throw new DomainException("El proveedor es obligatorio");
        if (destination == null || destination.isBlank())
            throw new DomainException("El destino/propósito es obligatorio");

        return new Request(UUID.randomUUID(), null, RequestStatus.BORRADOR, supplierId,
                solicitanteId, destination.trim(), authorizer, createdBy, List.of(), Instant.now(),
                null, null, null);
    }

    public void addItem(RequestItem item) {
        if (status == RequestStatus.CANCELADA)
            throw new DomainException("No se pueden agregar artículos a una solicitud cancelada");
        if (items.size() >= MAX_ITEMS) {
            throw new DomainException("Una solicitud no puede tener más de " + MAX_ITEMS + " renglones");
        }
        items.add(item);
    }

    public void removeItem(UUID itemId) {
        if (status == RequestStatus.CANCELADA)
            throw new DomainException("No se pueden eliminar artículos de una solicitud cancelada");
        items.removeIf(i -> i.getId().equals(itemId));
    }

    public void updateItem(UUID itemId, UUID workerId, String description,
                           BigDecimal quantity, String unit, BigDecimal unitCost) {
        if (status == RequestStatus.CANCELADA)
            throw new DomainException("No se pueden modificar artículos de una solicitud cancelada");
        RequestItem item = items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new DomainException("Artículo no encontrado"));
        item.update(workerId, description, quantity, unit, unitCost);
    }

    public void updateDraft(UUID supplierId, String destination, String authorizer) {
        ensureEditable();
        if (supplierId == null) throw new DomainException("El proveedor es obligatorio");
        if (destination == null || destination.isBlank())
            throw new DomainException("El destino/propósito es obligatorio");

        this.supplierId  = supplierId;
        this.destination = destination.trim();
        this.authorizer  = authorizer;
    }

    /** Permite corregir destino y autorizador en una solicitud ya emitida. */
    public void updateIssued(String destination, String authorizer) {
        if (status == RequestStatus.CANCELADA)
            throw new DomainException("No se puede modificar una solicitud cancelada");
        if (destination == null || destination.isBlank())
            throw new DomainException("El destino/propósito es obligatorio");

        this.destination = destination.trim();
        this.authorizer  = authorizer;
    }

    /**
     * Emite la solicitud asignando el folio generado de forma atómica en la capa de aplicación.
     * No se llama con folio=null nunca.
     */
    public void issue(long folio) {
        ensureEditable();
        if (items.isEmpty()) {
            throw new DomainException("La solicitud debe tener al menos un renglón antes de emitirse");
        }
        this.folio    = folio;
        this.status   = RequestStatus.EMITIDA;
        this.issuedAt = Instant.now();
        registerEvent(new RequestIssuedEvent(this.id, this.folio, this.supplierId, this.createdBy));
    }

    public void cancel(String reason, UUID cancelledBy) {
        if (status == RequestStatus.CANCELADA) {
            throw new DomainException("La solicitud ya está cancelada");
        }
        if (reason == null || reason.isBlank()) {
            throw new DomainException("El motivo de cancelación es obligatorio");
        }
        this.status              = RequestStatus.CANCELADA;
        this.cancelledAt         = Instant.now();
        this.cancellationReason  = reason.trim();
        registerEvent(new RequestCancelledEvent(this.id, this.folio, cancelledBy, reason));
    }

    /** Restaura una solicitud eliminada al estado previo inferido por su folio. */
    public void restore() {
        if (status != RequestStatus.CANCELADA) {
            throw new DomainException("Solo se pueden restaurar solicitudes canceladas");
        }
        this.status             = (folio != null || issuedAt != null) ? RequestStatus.EMITIDA : RequestStatus.BORRADOR;
        this.cancelledAt        = null;
        this.cancellationReason = null;
    }

    private void ensureEditable() {
        if (status != RequestStatus.BORRADOR) {
            throw new DomainException("Solo se pueden modificar solicitudes en estado BORRADOR");
        }
    }

    // --- Getters ---

    public UUID getId()                  { return id; }
    public Long getFolio()               { return folio; }
    public RequestStatus getStatus()     { return status; }
    public UUID getSupplierId()          { return supplierId; }
    public UUID getSolicitanteId()       { return solicitanteId; }
    public String getDestination()       { return destination; }
    public String getAuthorizer()        { return authorizer; }
    public UUID getCreatedBy()           { return createdBy; }
    public List<RequestItem> getItems()  { return Collections.unmodifiableList(items); }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getIssuedAt()         { return issuedAt; }
    public Instant getCancelledAt()      { return cancelledAt; }
    public String getCancellationReason(){ return cancellationReason; }
}

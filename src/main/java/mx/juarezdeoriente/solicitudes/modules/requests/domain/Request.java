package mx.juarezdeoriente.solicitudes.modules.requests.domain;
import mx.juarezdeoriente.solicitudes.modules.requests.domain.RequestStatus;
import mx.juarezdeoriente.solicitudes.modules.requests.domain.RequestItem;

import jakarta.persistence.*;
import mx.juarezdeoriente.solicitudes.exception.DomainException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "requests")
public class Request {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(unique = true)
    private Long folio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "supplier_id", nullable = false, columnDefinition = "uuid")
    private UUID supplierId;

    @Column(name = "solicitante_id", columnDefinition = "uuid")
    private UUID solicitanteId;

    @Column(nullable = false, columnDefinition = "text")
    private String destination;

    @Column(length = 200)
    private String authorizer;

    @Column(name = "created_by", nullable = false, columnDefinition = "uuid")
    private UUID createdBy;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<RequestItem> items = new ArrayList<>();

    private static final int MAX_ITEMS = 8;

    protected Request() {}

    public static Request create(UUID supplierId, UUID solicitanteId,
                                 String destination, String authorizer, UUID createdBy, long folio) {
        if (supplierId == null) throw new DomainException("El proveedor es obligatorio");
        if (destination == null || destination.isBlank())
            throw new DomainException("El destino/propósito es obligatorio");

        Request r = new Request();
        r.id            = UUID.randomUUID();
        r.folio         = folio;
        r.status        = RequestStatus.EMITIDA;
        r.supplierId    = supplierId;
        r.solicitanteId = solicitanteId;
        r.destination   = destination.trim();
        r.authorizer    = authorizer;
        r.createdBy     = createdBy;
        r.createdAt     = Instant.now();
        r.issuedAt      = r.createdAt;
        r.updatedAt     = r.createdAt;
        return r;
    }

    public void addItem(RequestItem item) {
        if (status == RequestStatus.CANCELADA)
            throw new DomainException("No se pueden agregar artículos a una solicitud cancelada");
        if (items.size() >= MAX_ITEMS) {
            throw new DomainException("Una solicitud no puede tener más de " + MAX_ITEMS + " renglones");
        }
        item.setRequest(this);
        items.add(item);
    }

    public void removeItem(UUID itemId) {
        if (status == RequestStatus.CANCELADA)
            throw new DomainException("No se pueden eliminar artículos de una solicitud cancelada");
        items.removeIf(i -> i.getId().equals(itemId));
    }

    public void updateItem(UUID itemId, UUID workerId, String description,
                           java.math.BigDecimal quantity, String unit, java.math.BigDecimal unitCost) {
        if (status == RequestStatus.CANCELADA)
            throw new DomainException("No se pueden modificar artículos de una solicitud cancelada");
        RequestItem item = items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new DomainException("Artículo no encontrado"));
        item.update(workerId, description, quantity, unit, unitCost);
    }

    public void update(UUID supplierId, UUID solicitanteId, String destination, String authorizer) {
        if (status == RequestStatus.CANCELADA)
            throw new DomainException("No se puede modificar una solicitud cancelada");
        if (supplierId != null) this.supplierId = supplierId;
        this.solicitanteId = solicitanteId;
        if (destination != null && !destination.isBlank()) this.destination = destination.trim();
        this.authorizer = authorizer;
        this.updatedAt  = Instant.now();
    }

    public void cancel(String reason, UUID cancelledBy) {
        if (status == RequestStatus.CANCELADA) {
            throw new DomainException("La solicitud ya está cancelada");
        }
        if (reason == null || reason.isBlank()) {
            throw new DomainException("El motivo de cancelación es obligatorio");
        }
        this.status             = RequestStatus.CANCELADA;
        this.cancelledAt        = Instant.now();
        this.cancellationReason = reason.trim();
        this.updatedAt          = this.cancelledAt;
    }

    public void restore() {
        if (status != RequestStatus.CANCELADA) {
            throw new DomainException("Solo se pueden restaurar solicitudes canceladas");
        }
        this.status             = RequestStatus.EMITIDA;
        this.cancelledAt        = null;
        this.cancellationReason = null;
        this.updatedAt          = Instant.now();
    }

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

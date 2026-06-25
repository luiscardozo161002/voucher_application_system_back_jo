package mx.juarezdeoriente.solicitudes.requests.domain.event;

import mx.juarezdeoriente.solicitudes.shared.domain.model.DomainEvent;

import java.util.UUID;

public class RequestIssuedEvent extends DomainEvent {

    private final UUID requestId;
    private final long folio;
    private final UUID supplierId;
    private final UUID issuedBy;

    public RequestIssuedEvent(UUID requestId, long folio, UUID supplierId, UUID issuedBy) {
        super();
        this.requestId  = requestId;
        this.folio      = folio;
        this.supplierId = supplierId;
        this.issuedBy   = issuedBy;
    }

    public UUID getRequestId()  { return requestId; }
    public long getFolio()      { return folio; }
    public UUID getSupplierId() { return supplierId; }
    public UUID getIssuedBy()   { return issuedBy; }
}

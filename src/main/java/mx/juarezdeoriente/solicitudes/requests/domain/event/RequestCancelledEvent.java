package mx.juarezdeoriente.solicitudes.requests.domain.event;

import mx.juarezdeoriente.solicitudes.shared.domain.model.DomainEvent;

import java.util.UUID;

public class RequestCancelledEvent extends DomainEvent {

    private final UUID requestId;
    private final Long folio;
    private final UUID cancelledBy;
    private final String reason;

    public RequestCancelledEvent(UUID requestId, Long folio, UUID cancelledBy, String reason) {
        super();
        this.requestId   = requestId;
        this.folio       = folio;
        this.cancelledBy = cancelledBy;
        this.reason      = reason;
    }

    public UUID getRequestId()  { return requestId; }
    public Long getFolio()      { return folio; }
    public UUID getCancelledBy(){ return cancelledBy; }
    public String getReason()   { return reason; }
}

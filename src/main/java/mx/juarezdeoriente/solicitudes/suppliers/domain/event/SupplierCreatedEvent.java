package mx.juarezdeoriente.solicitudes.suppliers.domain.event;

import mx.juarezdeoriente.solicitudes.shared.domain.model.DomainEvent;

import java.util.UUID;

public class SupplierCreatedEvent extends DomainEvent {

    private final UUID supplierId;
    private final String code;
    private final String name;
    private final UUID actorId;

    public SupplierCreatedEvent(UUID supplierId, String code, String name, UUID actorId) {
        super();
        this.supplierId = supplierId;
        this.code       = code;
        this.name       = name;
        this.actorId    = actorId;
    }

    public UUID getSupplierId() { return supplierId; }
    public String getCode()     { return code; }
    public String getName()     { return name; }
    public UUID getActorId()    { return actorId; }
}

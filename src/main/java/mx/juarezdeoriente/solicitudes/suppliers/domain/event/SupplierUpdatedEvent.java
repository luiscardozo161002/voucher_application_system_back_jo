package mx.juarezdeoriente.solicitudes.suppliers.domain.event;

import mx.juarezdeoriente.solicitudes.shared.domain.model.DomainEvent;

import java.util.UUID;

public class SupplierUpdatedEvent extends DomainEvent {

    private final UUID supplierId;
    private final String name;

    public SupplierUpdatedEvent(UUID supplierId, String name) {
        super();
        this.supplierId = supplierId;
        this.name       = name;
    }

    public UUID getSupplierId() { return supplierId; }
    public String getName()     { return name; }
}

package mx.juarezdeoriente.solicitudes.suppliers.domain.model;

import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.domain.model.AggregateRoot;
import mx.juarezdeoriente.solicitudes.suppliers.domain.event.SupplierCreatedEvent;
import mx.juarezdeoriente.solicitudes.suppliers.domain.event.SupplierUpdatedEvent;

import java.time.Instant;
import java.util.UUID;

public class Supplier extends AggregateRoot {

    private final UUID id;
    private String code;
    private String name;
    private String phone;
    private boolean active;
    private final Instant createdAt;

    private Supplier(UUID id, String code, String name, String phone, boolean active, Instant createdAt) {
        this.id        = id;
        this.code      = code;
        this.name      = name;
        this.phone     = phone;
        this.active    = active;
        this.createdAt = createdAt;
    }

    public static Supplier reconstitute(UUID id, String code, String name,
                                        String phone, boolean active, Instant createdAt) {
        return new Supplier(id, code, name, phone, active, createdAt);
    }

    public static Supplier create(String code, String name, String phone) {
        if (code == null || code.isBlank()) throw new DomainException("La clave del proveedor es obligatoria");
        if (name == null || name.isBlank()) throw new DomainException("El nombre del proveedor es obligatorio");

        Supplier s = new Supplier(UUID.randomUUID(), code.trim().toUpperCase(), name.trim(),
                phone, true, Instant.now());
        s.registerEvent(new SupplierCreatedEvent(s.id, s.code, s.name));
        return s;
    }

    public void update(String name, String phone) {
        if (name == null || name.isBlank()) throw new DomainException("El nombre del proveedor es obligatorio");
        this.name  = name.trim();
        this.phone = phone;
        registerEvent(new SupplierUpdatedEvent(this.id, this.name));
    }

    public void deactivate() { this.active = false; }
    public void activate()   { this.active = true; }

    public UUID getId()        { return id; }
    public String getCode()    { return code; }
    public String getName()    { return name; }
    public String getPhone()   { return phone; }
    public boolean isActive()  { return active; }
    public Instant getCreatedAt() { return createdAt; }
}

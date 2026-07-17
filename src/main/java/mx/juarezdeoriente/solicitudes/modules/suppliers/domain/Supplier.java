package mx.juarezdeoriente.solicitudes.modules.suppliers.domain;
import mx.juarezdeoriente.solicitudes.exception.ConflictException;

import jakarta.persistence.*;
import mx.juarezdeoriente.solicitudes.exception.DomainException;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    private UUID id;

    @Column(unique = true, length = 50, nullable = false)
    private String code;

    @Column(length = 300, nullable = false)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected Supplier() {}

    public static Supplier create(String code, String name, String phone) {
        if (code == null || code.isBlank())
            throw new DomainException("La clave del proveedor es obligatoria");
        if (name == null || name.isBlank())
            throw new DomainException("El nombre del proveedor es obligatorio");

        Supplier s = new Supplier();
        s.id        = UUID.randomUUID();
        s.code      = code.trim().toUpperCase();
        s.name      = name.trim();
        s.phone     = normalize(phone);
        s.active    = true;
        s.createdAt = Instant.now();
        s.updatedAt = s.createdAt;
        return s;
    }

    public void update(String name, String phone) {
        if (name == null || name.isBlank())
            throw new DomainException("El nombre del proveedor es obligatorio");
        this.name      = name.trim();
        this.phone     = normalize(phone);
        this.updatedAt = Instant.now();
    }

    public void toggleActive(boolean active) {
        this.active    = active;
        this.updatedAt = Instant.now();
    }

    // ── getters ──────────────────────────────────────────────
    public UUID    getId()        { return id; }
    public String  getCode()      { return code; }
    public String  getName()      { return name; }
    public String  getPhone()     { return phone; }
    public boolean isActive()     { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static String normalize(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }
}

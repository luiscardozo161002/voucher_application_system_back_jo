package mx.juarezdeoriente.solicitudes.suppliers.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "suppliers")
public class SupplierJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected SupplierJpaEntity() {}

    public UUID getId()                  { return id; }
    public void setId(UUID v)            { this.id = v; }
    public String getCode()              { return code; }
    public void setCode(String v)        { this.code = v; }
    public String getName()              { return name; }
    public void setName(String v)        { this.name = v; }
    public String getPhone()             { return phone; }
    public void setPhone(String v)       { this.phone = v; }
    public boolean isActive()            { return active; }
    public void setActive(boolean v)     { this.active = v; }
    public Long getVersion()             { return version; }
    public void setVersion(Long v)       { this.version = v; }
    public Instant getCreatedAt()        { return createdAt; }
    public void setCreatedAt(Instant v)  { this.createdAt = v; }
    public Instant getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(Instant v)  { this.updatedAt = v; }
}

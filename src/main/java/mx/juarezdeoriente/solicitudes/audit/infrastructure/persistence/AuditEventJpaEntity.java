package mx.juarezdeoriente.solicitudes.audit.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", columnDefinition = "uuid")
    private UUID actorId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(columnDefinition = "text")
    private String changes;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEventJpaEntity() {}

    public Long getId()                    { return id; }
    public UUID getActorId()               { return actorId; }
    public void setActorId(UUID v)         { this.actorId = v; }
    public String getAction()              { return action; }
    public void setAction(String v)        { this.action = v; }
    public String getEntityType()          { return entityType; }
    public void setEntityType(String v)    { this.entityType = v; }
    public String getEntityId()            { return entityId; }
    public void setEntityId(String v)      { this.entityId = v; }
    public String getChanges()             { return changes; }
    public void setChanges(String v)       { this.changes = v; }
    public String getIpAddress()           { return ipAddress; }
    public void setIpAddress(String v)     { this.ipAddress = v; }
    public Instant getOccurredAt()         { return occurredAt; }
    public void setOccurredAt(Instant v)   { this.occurredAt = v; }
}

package mx.juarezdeoriente.solicitudes.audit;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

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

    protected AuditEvent() {}

    public static AuditEvent create(UUID actorId, String action, String entityType,
                                    String entityId, String changes) {
        AuditEvent e = new AuditEvent();
        e.actorId    = actorId;
        e.action     = action;
        e.entityType = entityType;
        e.entityId   = entityId;
        e.changes    = changes;
        e.occurredAt = Instant.now();
        return e;
    }

    public Long getId()               { return id; }
    public UUID getActorId()          { return actorId; }
    public String getAction()         { return action; }
    public String getEntityType()     { return entityType; }
    public String getEntityId()       { return entityId; }
    public String getChanges()        { return changes; }
    public String getIpAddress()      { return ipAddress; }
    public Instant getOccurredAt()    { return occurredAt; }
}

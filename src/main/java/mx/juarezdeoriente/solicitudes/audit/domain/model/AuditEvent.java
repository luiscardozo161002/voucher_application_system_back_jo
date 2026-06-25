package mx.juarezdeoriente.solicitudes.audit.domain.model;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

public class AuditEvent {

    private final Long id;
    private final UUID actorId;
    private final String action;
    private final String entityType;
    private final String entityId;
    private final String changes;
    private final String ipAddress;
    private final Instant occurredAt;

    private AuditEvent(Long id, UUID actorId, String action, String entityType,
                       String entityId, String changes, String ipAddress, Instant occurredAt) {
        this.id          = id;
        this.actorId     = actorId;
        this.action      = action;
        this.entityType  = entityType;
        this.entityId    = entityId;
        this.changes     = changes;
        this.ipAddress   = ipAddress;
        this.occurredAt  = occurredAt;
    }

    public static AuditEvent reconstitute(Long id, UUID actorId, String action,
                                          String entityType, String entityId,
                                          String changes, String ipAddress, Instant occurredAt) {
        return new AuditEvent(id, actorId, action, entityType, entityId, changes, ipAddress, occurredAt);
    }

    public static AuditEvent create(UUID actorId, String action, String entityType,
                                    String entityId, String changes) {
        return new AuditEvent(null, actorId, action, entityType, entityId,
                changes, null, Instant.now());
    }

    public Long getId()           { return id; }
    public UUID getActorId()      { return actorId; }
    public String getAction()     { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId()   { return entityId; }
    public String getChanges()    { return changes; }
    public String getIpAddress()  { return ipAddress; }
    public Instant getOccurredAt(){ return occurredAt; }
}

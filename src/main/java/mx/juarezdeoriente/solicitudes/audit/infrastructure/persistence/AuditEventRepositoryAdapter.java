package mx.juarezdeoriente.solicitudes.audit.infrastructure.persistence;

import mx.juarezdeoriente.solicitudes.audit.domain.model.AuditEvent;
import mx.juarezdeoriente.solicitudes.audit.domain.port.AuditEventRepository;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
class AuditEventRepositoryAdapter implements AuditEventRepository {

    private final AuditEventJpaRepository jpa;

    AuditEventRepositoryAdapter(AuditEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuditEvent save(AuditEvent event) {
        AuditEventJpaEntity e = new AuditEventJpaEntity();
        e.setActorId(event.getActorId());
        e.setAction(event.getAction());
        e.setEntityType(event.getEntityType());
        e.setEntityId(event.getEntityId());
        e.setChanges(event.getChanges());
        e.setIpAddress(event.getIpAddress());
        e.setOccurredAt(event.getOccurredAt());
        AuditEventJpaEntity saved = jpa.save(e);
        return toDomain(saved);
    }

    @Override
    public PageResult<AuditEvent> search(UUID actorId, String action, String entityType,
                                         Instant from, Instant to, int page, int size) {
        Page<AuditEventJpaEntity> p = jpa.search(actorId, action, entityType, from, to,
                PageRequest.of(page, size));
        return PageResult.of(p.getContent().stream().map(this::toDomain).toList(),
                page, size, p.getTotalElements());
    }

    private AuditEvent toDomain(AuditEventJpaEntity e) {
        return AuditEvent.reconstitute(e.getId(), e.getActorId(), e.getAction(),
                e.getEntityType(), e.getEntityId(), e.getChanges(),
                e.getIpAddress(), e.getOccurredAt());
    }
}

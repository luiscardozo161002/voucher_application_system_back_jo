package mx.juarezdeoriente.solicitudes.audit.infrastructure.persistence;

import jakarta.persistence.criteria.Predicate;
import mx.juarezdeoriente.solicitudes.audit.domain.model.AuditEvent;
import mx.juarezdeoriente.solicitudes.audit.domain.port.AuditEventRepository;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
        return toDomain(jpa.save(e));
    }

    @Override
    public PageResult<AuditEvent> search(UUID actorId, String action, String entityType,
                                         Instant from, Instant to, int page, int size) {
        Specification<AuditEventJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actorId != null)    predicates.add(cb.equal(root.get("actorId"), actorId));
            if (action != null)     predicates.add(cb.equal(root.get("action"), action));
            if (entityType != null) predicates.add(cb.equal(root.get("entityType"), entityType));
            if (from != null)       predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null)         predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AuditEventJpaEntity> p = jpa.findAll(spec, pageable);
        return PageResult.of(p.getContent().stream().map(this::toDomain).toList(),
                page, size, p.getTotalElements());
    }

    private AuditEvent toDomain(AuditEventJpaEntity e) {
        return AuditEvent.reconstitute(e.getId(), e.getActorId(), e.getAction(),
                e.getEntityType(), e.getEntityId(), e.getChanges(),
                e.getIpAddress(), e.getOccurredAt());
    }
}

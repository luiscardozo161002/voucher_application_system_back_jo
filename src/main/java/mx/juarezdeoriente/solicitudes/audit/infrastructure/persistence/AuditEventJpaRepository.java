package mx.juarezdeoriente.solicitudes.audit.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface AuditEventJpaRepository extends JpaRepository<AuditEventJpaEntity, Long> {

    @Query("""
            SELECT a FROM AuditEventJpaEntity a
            WHERE (:actorId IS NULL OR a.actorId = :actorId)
              AND (:action IS NULL OR a.action = :action)
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:from IS NULL OR a.occurredAt >= :from)
              AND (:to IS NULL OR a.occurredAt <= :to)
            ORDER BY a.occurredAt DESC
            """)
    Page<AuditEventJpaEntity> search(@Param("actorId") UUID actorId,
                                     @Param("action") String action,
                                     @Param("entityType") String entityType,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);
}

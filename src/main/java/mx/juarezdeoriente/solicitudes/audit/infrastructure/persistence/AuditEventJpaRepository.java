package mx.juarezdeoriente.solicitudes.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface AuditEventJpaRepository
        extends JpaRepository<AuditEventJpaEntity, Long>,
                JpaSpecificationExecutor<AuditEventJpaEntity> {
}

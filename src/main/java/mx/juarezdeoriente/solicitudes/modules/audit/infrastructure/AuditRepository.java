package mx.juarezdeoriente.solicitudes.modules.audit.infrastructure;
import mx.juarezdeoriente.solicitudes.modules.audit.domain.AuditEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditRepository extends JpaRepository<AuditEvent, Long>,
                                          JpaSpecificationExecutor<AuditEvent> {
}

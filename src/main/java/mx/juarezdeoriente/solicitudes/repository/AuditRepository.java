package mx.juarezdeoriente.solicitudes.repository;
import mx.juarezdeoriente.solicitudes.model.AuditEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditRepository extends JpaRepository<AuditEvent, Long>,
                                          JpaSpecificationExecutor<AuditEvent> {
}

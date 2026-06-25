package mx.juarezdeoriente.solicitudes.audit.domain.port;

import mx.juarezdeoriente.solicitudes.audit.domain.model.AuditEvent;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;

import java.time.Instant;
import java.util.UUID;

public interface AuditEventRepository {

    AuditEvent save(AuditEvent event);

    PageResult<AuditEvent> search(UUID actorId, String action, String entityType,
                                  Instant from, Instant to, int page, int size);
}

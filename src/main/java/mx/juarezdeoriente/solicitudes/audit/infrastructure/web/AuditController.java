package mx.juarezdeoriente.solicitudes.audit.infrastructure.web;

import mx.juarezdeoriente.solicitudes.audit.domain.model.AuditEvent;
import mx.juarezdeoriente.solicitudes.audit.domain.port.AuditEventRepository;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.shared.infrastructure.web.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-events")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
public class AuditController {

    private final AuditEventRepository auditEventRepository;

    public AuditController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<AuditEvent>>> search(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                auditEventRepository.search(actorId, action, entityType, from, to, page, size)
        ));
    }
}

package mx.juarezdeoriente.solicitudes.modules.audit.presentation;
import mx.juarezdeoriente.solicitudes.modules.audit.infrastructure.AuditRepository;
import mx.juarezdeoriente.solicitudes.modules.audit.domain.AuditEvent;

import jakarta.persistence.criteria.Predicate;
import mx.juarezdeoriente.solicitudes.shared.PageResult;
import mx.juarezdeoriente.solicitudes.shared.web.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-events")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
public class AuditController {

    private final AuditRepository auditRepository;

    public AuditController(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
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

        Specification<AuditEvent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actorId != null)    predicates.add(cb.equal(root.get("actorId"), actorId));
            if (action != null)     predicates.add(cb.equal(root.get("action"), action));
            if (entityType != null) predicates.add(cb.equal(root.get("entityType"), entityType));
            if (from != null)       predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null)         predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AuditEvent> p = auditRepository.findAll(spec, pageable);
        PageResult<AuditEvent> result = PageResult.of(p.getContent(), page, size, p.getTotalElements());

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}

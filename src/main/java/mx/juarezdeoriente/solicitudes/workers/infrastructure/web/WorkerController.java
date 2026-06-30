package mx.juarezdeoriente.solicitudes.workers.infrastructure.web;

import jakarta.validation.Valid;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.shared.infrastructure.web.ApiResponse;
import mx.juarezdeoriente.solicitudes.workers.application.service.WorkerService;
import mx.juarezdeoriente.solicitudes.workers.infrastructure.web.dto.WorkerRequest;
import mx.juarezdeoriente.solicitudes.workers.infrastructure.web.dto.WorkerResponse;
import mx.juarezdeoriente.solicitudes.workers.infrastructure.web.dto.WorkerUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<WorkerResponse>> create(@Valid @RequestBody WorkerRequest req) {
        var worker = workerService.create(req.companyCode(), req.employeeNumber(),
                req.name(), req.phone(), req.workerType());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(WorkerResponse.from(worker)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<WorkerResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) mx.juarezdeoriente.solicitudes.workers.domain.model.WorkerType workerType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResult<WorkerResponse> result = workerService
                .search(q, active, workerType, Math.max(0, page), Math.min(size < 1 ? 10 : size, 100))
                .map(WorkerResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkerResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(WorkerResponse.from(workerService.findById(id))));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<WorkerResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody WorkerUpdateRequest req) {

        if (req.active() != null) {
            workerService.setActive(id, req.active());
        }
        var worker = workerService.update(id, req.companyCode(), req.employeeNumber(),
                req.name(), req.phone(), req.workerType());
        return ResponseEntity.ok(ApiResponse.ok(WorkerResponse.from(worker)));
    }
}

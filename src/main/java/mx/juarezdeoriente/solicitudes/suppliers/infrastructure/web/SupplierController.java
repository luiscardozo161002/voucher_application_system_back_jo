package mx.juarezdeoriente.solicitudes.suppliers.infrastructure.web;

import jakarta.validation.Valid;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.AppUserDetails;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.shared.infrastructure.web.ApiResponse;
import mx.juarezdeoriente.solicitudes.suppliers.application.service.SupplierService;
import mx.juarezdeoriente.solicitudes.suppliers.infrastructure.web.dto.SupplierRequest;
import mx.juarezdeoriente.solicitudes.suppliers.infrastructure.web.dto.SupplierResponse;
import mx.juarezdeoriente.solicitudes.suppliers.infrastructure.web.dto.SupplierUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<SupplierResponse>> create(
            @Valid @RequestBody SupplierRequest req,
            @AuthenticationPrincipal AppUserDetails principal) {
        var supplier = supplierService.create(req.code(), req.name(), req.phone(),
                principal != null ? principal.getId() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(SupplierResponse.from(supplier)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<SupplierResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResult<SupplierResponse> result = supplierService.search(q, active, Math.max(0, page), Math.min(size < 1 ? 10 : size, 500))
                .map(SupplierResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(SupplierResponse.from(supplierService.findById(id))));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<SupplierResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierUpdateRequest req,
            @AuthenticationPrincipal AppUserDetails principal) {

        if (req.active() != null && req.name() == null && req.phone() == null) {
            var supplier = supplierService.setActive(id, req.active());
            return ResponseEntity.ok(ApiResponse.ok(SupplierResponse.from(supplier)));
        }
        if (req.active() != null) {
            supplierService.setActive(id, req.active());
        }
        var supplier = supplierService.update(id, req.name(), req.phone(),
                principal != null ? principal.getId() : null);
        return ResponseEntity.ok(ApiResponse.ok(SupplierResponse.from(supplier)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails principal) {
        supplierService.delete(id, principal != null ? principal.getId() : null);
        return ResponseEntity.noContent().build();
    }
}

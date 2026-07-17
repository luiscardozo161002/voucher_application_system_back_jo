package mx.juarezdeoriente.solicitudes.suppliers;
import mx.juarezdeoriente.solicitudes.suppliers.dto.SupplierDto;
import mx.juarezdeoriente.solicitudes.suppliers.SupplierService;
import mx.juarezdeoriente.solicitudes.suppliers.Supplier;

import jakarta.validation.Valid;
import mx.juarezdeoriente.solicitudes.security.AppUserDetails;
import mx.juarezdeoriente.solicitudes.shared.PageResult;
import mx.juarezdeoriente.solicitudes.shared.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<SupplierDto.Response>> create(
            @Valid @RequestBody SupplierDto.CreateRequest req,
            @AuthenticationPrincipal AppUserDetails principal) {
        var supplier = service.create(req.code(), req.name(), req.phone(),
                principal != null ? principal.getId() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(SupplierDto.Response.from(supplier)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<SupplierDto.Response>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<SupplierDto.Response> result = service
                .search(q, active, Math.max(0, page), Math.min(size < 1 ? 10 : size, 500))
                .map(SupplierDto.Response::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDto.Response>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(SupplierDto.Response.from(service.findById(id))));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<SupplierDto.Response>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierDto.UpdateRequest req,
            @AuthenticationPrincipal AppUserDetails principal) {

        if (req.active() != null && req.name() == null && req.phone() == null) {
            return ResponseEntity.ok(ApiResponse.ok(SupplierDto.Response.from(service.setActive(id, req.active()))));
        }
        if (req.active() != null) {
            service.setActive(id, req.active());
        }
        var supplier = service.update(id, req.name(), req.phone(),
                principal != null ? principal.getId() : null);
        return ResponseEntity.ok(ApiResponse.ok(SupplierDto.Response.from(supplier)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails principal) {
        service.delete(id, principal != null ? principal.getId() : null);
        return ResponseEntity.noContent().build();
    }
}

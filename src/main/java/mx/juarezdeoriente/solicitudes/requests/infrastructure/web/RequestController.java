package mx.juarezdeoriente.solicitudes.requests.infrastructure.web;

import jakarta.validation.Valid;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.AppUserDetails;
import mx.juarezdeoriente.solicitudes.requests.application.service.RequestService;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestStatus;
import mx.juarezdeoriente.solicitudes.requests.infrastructure.web.dto.*;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.shared.infrastructure.web.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestResponse>> create(
            @Valid @RequestBody CreateDraftRequest req,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        var request = requestService.createDraft(
                req.supplierId(), req.destination(), req.authorizer(), currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(RequestResponse.from(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<RequestResponse>>> search(
            @RequestParam(required = false) String folio,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) UUID workerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<RequestResponse> result = requestService
                .search(folio, supplierId, workerId, from, to, status, createdBy, Math.max(0, page), Math.min(size < 1 ? 20 : size, 100))
                .map(RequestResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(RequestResponse.from(requestService.findById(id))));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestResponse>> updateDraft(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDraftRequest req) {

        var request = requestService.updateDraft(id, req.supplierId(), req.destination(), req.authorizer());
        return ResponseEntity.ok(ApiResponse.ok(RequestResponse.from(request)));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestResponse>> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody AddItemRequest req) {

        var request = requestService.addItem(
                id, req.workerId(), req.description(), req.quantity(), req.unit(), req.unitCost());
        return ResponseEntity.ok(ApiResponse.ok(RequestResponse.from(request)));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestResponse>> removeItem(
            @PathVariable UUID id, @PathVariable UUID itemId) {

        return ResponseEntity.ok(ApiResponse.ok(RequestResponse.from(requestService.removeItem(id, itemId))));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestResponse>> issue(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(RequestResponse.from(requestService.issue(id))));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestResponse>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelRequest req,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        var request = requestService.cancel(id, req.reason(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(RequestResponse.from(request)));
    }
}

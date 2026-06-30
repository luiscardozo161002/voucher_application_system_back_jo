package mx.juarezdeoriente.solicitudes.requests.infrastructure.web;

import jakarta.validation.Valid;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.AppUserDetails;
import mx.juarezdeoriente.solicitudes.requests.application.service.RequestService;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestStatus;
import mx.juarezdeoriente.solicitudes.requests.infrastructure.web.dto.*;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.shared.infrastructure.security.SecurityHelper;
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

    private final RequestService  requestService;
    private final SecurityHelper  security;

    public RequestController(RequestService requestService, SecurityHelper security) {
        this.requestService = requestService;
        this.security       = security;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestResponse>> create(
            @Valid @RequestBody CreateAndIssueRequest req,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        var items = req.items().stream()
                .map(i -> new mx.juarezdeoriente.solicitudes.requests.application.service.RequestService.ItemData(
                        i.workerId(), i.description(), i.quantity(), i.unit(), i.unitCost()))
                .toList();

        var request = requestService.createAndIssue(
                req.supplierId(), req.solicitanteId(), req.destination(),
                req.authorizer(), currentUser.getId(), items);
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean excludeCancelled,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        UUID effectiveCreatedBy = security.isCapturistaOnly(currentUser)
                ? currentUser.getId()
                : createdBy;

        PageResult<RequestResponse> result = requestService
                .search(folio, supplierId, workerId, from, to, status, effectiveCreatedBy,
                        Math.max(0, page), Math.min(size < 1 ? 10 : size, 100), excludeCancelled)
                .map(RequestResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        var request = requestService.findById(id);

        if (security.isCapturistaOnly(currentUser) && !request.getCreatedBy().equals(currentUser.getId())) {
            throw new DomainException(SecurityHelper.ACCESS_DENIED);
        }

        return ResponseEntity.ok(ApiResponse.ok(RequestResponse.from(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestResponse>> updateRequest(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDraftRequest req) {

        var request = requestService.updateRequest(id, req.supplierId(), req.destination(), req.authorizer());
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

    @PatchMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestResponse>> updateItem(
            @PathVariable UUID id, @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemRequest req) {

        var request = requestService.updateItem(
                id, itemId, req.workerId(), req.description(), req.quantity(), req.unit(), req.unitCost());
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

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> restore(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        var request = requestService.findById(id);
        if (security.isCapturistaOnly(currentUser) && !request.getCreatedBy().equals(currentUser.getId())) {
            throw new DomainException(SecurityHelper.ACCESS_DENIED);
        }
        return ResponseEntity.ok(ApiResponse.ok(RequestResponse.from(requestService.restore(id))));
    }

    @DeleteMapping("/trash/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAllCancelled(
            @AuthenticationPrincipal AppUserDetails currentUser) {

        UUID filterBy = security.ownerFilter(currentUser);
        requestService.deleteAllCancelled(filterBy);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePermanently(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        var request = requestService.findById(id);
        if (security.isCapturistaOnly(currentUser) && !request.getCreatedBy().equals(currentUser.getId())) {
            throw new DomainException(SecurityHelper.ACCESS_DENIED);
        }
        requestService.deletePermanently(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<RequestStatsResponse>> stats(
            @AuthenticationPrincipal AppUserDetails currentUser) {

        UUID filterBy = security.ownerFilter(currentUser);

        long borradores = requestService.countByStatus(RequestStatus.BORRADOR,  filterBy);
        long emitidas   = requestService.countByStatus(RequestStatus.EMITIDA,   filterBy);
        long canceladas = requestService.countByStatus(RequestStatus.CANCELADA, filterBy);
        var  recent     = requestService.search(null, null, null, null, null, null, filterBy, 0, 5, true).content();

        return ResponseEntity.ok(ApiResponse.ok(new RequestStatsResponse(
                borradores, emitidas, canceladas,
                recent.stream().map(RequestResponse::from).toList()
        )));
    }
}

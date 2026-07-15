package mx.juarezdeoriente.solicitudes.controller;
import mx.juarezdeoriente.solicitudes.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.dto.RequestDto;
import mx.juarezdeoriente.solicitudes.service.RequestService;
import mx.juarezdeoriente.solicitudes.model.RequestStatus;
import mx.juarezdeoriente.solicitudes.model.Request;

import jakarta.validation.Valid;
import mx.juarezdeoriente.solicitudes.security.AppUserDetails;
import mx.juarezdeoriente.solicitudes.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.PageResult;
import mx.juarezdeoriente.solicitudes.security.SecurityHelper;
import mx.juarezdeoriente.solicitudes.shared.web.ApiResponse;
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
    private final SecurityHelper security;

    public RequestController(RequestService requestService, SecurityHelper security) {
        this.requestService = requestService;
        this.security       = security;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestDto.Response>> create(
            @Valid @RequestBody RequestDto.CreateAndIssueRequest req,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        var items = req.items().stream()
                .map(i -> new RequestService.ItemData(
                        i.workerId(), i.description(), i.quantity(), i.unit(), i.unitCost()))
                .toList();

        var request = requestService.createAndIssue(
                req.supplierId(), req.solicitanteId(), req.destination(),
                req.authorizer(), currentUser.getId(), items);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(RequestDto.Response.from(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<RequestDto.Response>>> search(
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

        PageResult<RequestDto.Response> result = requestService
                .search(folio, supplierId, workerId, from, to, status, effectiveCreatedBy,
                        Math.max(0, page), Math.min(size < 1 ? 10 : size, 100), excludeCancelled)
                .map(RequestDto.Response::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestDto.Response>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        var request = requestService.findById(id);

        if (security.isCapturistaOnly(currentUser) && !request.getCreatedBy().equals(currentUser.getId())) {
            throw new DomainException(SecurityHelper.ACCESS_DENIED);
        }

        return ResponseEntity.ok(ApiResponse.ok(RequestDto.Response.from(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestDto.Response>> updateRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RequestDto.CreateDraftRequest req) {

        var request = requestService.updateRequest(id, req.supplierId(), req.destination(), req.authorizer());
        return ResponseEntity.ok(ApiResponse.ok(RequestDto.Response.from(request)));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestDto.Response>> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody RequestDto.AddItemRequest req) {

        var request = requestService.addItem(
                id, req.workerId(), req.description(), req.quantity(), req.unit(), req.unitCost());
        return ResponseEntity.ok(ApiResponse.ok(RequestDto.Response.from(request)));
    }

    @PatchMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestDto.Response>> updateItem(
            @PathVariable UUID id, @PathVariable UUID itemId,
            @Valid @RequestBody RequestDto.UpdateItemRequest req) {

        var request = requestService.updateItem(
                id, itemId, req.workerId(), req.description(), req.quantity(), req.unit(), req.unitCost());
        return ResponseEntity.ok(ApiResponse.ok(RequestDto.Response.from(request)));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestDto.Response>> removeItem(
            @PathVariable UUID id, @PathVariable UUID itemId) {

        return ResponseEntity.ok(ApiResponse.ok(RequestDto.Response.from(requestService.removeItem(id, itemId))));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestDto.Response>> issue(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(RequestDto.Response.from(requestService.issue(id))));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTURISTA')")
    public ResponseEntity<ApiResponse<RequestDto.Response>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody RequestDto.CancelRequest req,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        var request = requestService.cancel(id, req.reason(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(RequestDto.Response.from(request)));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RequestDto.Response>> restore(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        var request = requestService.findById(id);
        if (security.isCapturistaOnly(currentUser) && !request.getCreatedBy().equals(currentUser.getId())) {
            throw new DomainException(SecurityHelper.ACCESS_DENIED);
        }
        return ResponseEntity.ok(ApiResponse.ok(RequestDto.Response.from(requestService.restore(id))));
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
    public ResponseEntity<ApiResponse<RequestDto.StatsResponse>> stats(
            @AuthenticationPrincipal AppUserDetails currentUser) {

        UUID filterBy = security.ownerFilter(currentUser);

        long borradores = requestService.countByStatus(RequestStatus.BORRADOR,  filterBy);
        long emitidas   = requestService.countByStatus(RequestStatus.EMITIDA,   filterBy);
        long canceladas = requestService.countByStatus(RequestStatus.CANCELADA, filterBy);
        var  recent     = requestService.search(null, null, null, null, null, null, filterBy, 0, 5, true).content();

        return ResponseEntity.ok(ApiResponse.ok(new RequestDto.StatsResponse(
                borradores, emitidas, canceladas,
                recent.stream().map(RequestDto.Response::from).toList()
        )));
    }
}

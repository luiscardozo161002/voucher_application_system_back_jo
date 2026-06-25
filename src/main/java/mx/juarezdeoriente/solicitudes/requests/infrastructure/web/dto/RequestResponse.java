package mx.juarezdeoriente.solicitudes.requests.infrastructure.web.dto;

import mx.juarezdeoriente.solicitudes.requests.domain.model.Request;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RequestResponse(
        UUID id,
        Long folio,
        String folioFormatted,
        RequestStatus status,
        UUID supplierId,
        String destination,
        String authorizer,
        UUID createdBy,
        List<RequestItemResponse> items,
        Instant createdAt,
        Instant issuedAt,
        Instant cancelledAt,
        String cancellationReason
) {
    public static RequestResponse from(Request r) {
        String formatted = r.getFolio() != null
                ? String.format("%07d", r.getFolio())
                : null;

        return new RequestResponse(
                r.getId(), r.getFolio(), formatted, r.getStatus(),
                r.getSupplierId(), r.getDestination(), r.getAuthorizer(),
                r.getCreatedBy(),
                r.getItems().stream().map(RequestItemResponse::from).toList(),
                r.getCreatedAt(), r.getIssuedAt(),
                r.getCancelledAt(), r.getCancellationReason()
        );
    }
}

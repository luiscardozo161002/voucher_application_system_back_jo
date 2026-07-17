package mx.juarezdeoriente.solicitudes.requests.dto;
import mx.juarezdeoriente.solicitudes.requests.RequestStatus;
import mx.juarezdeoriente.solicitudes.requests.RequestItem;
import mx.juarezdeoriente.solicitudes.requests.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RequestDto {

    public record CreateAndIssueRequest(
            @NotNull(message = "El proveedor es obligatorio") UUID supplierId,
            UUID solicitanteId,
            @NotBlank(message = "El destino es obligatorio")
            @Size(max = 1000) String destination,
            String authorizer,
            @NotEmpty(message = "Debe agregar al menos un artículo") List<AddItemRequest> items
    ) {}

    public record CreateDraftRequest(
            @NotNull(message = "El proveedor es obligatorio") UUID supplierId,
            UUID solicitanteId,
            @NotBlank(message = "El destino/proposito es obligatorio")
            @Size(max = 1000, message = "El destino no puede exceder 1000 caracteres") String destination,
            String authorizer
    ) {}

    public record AddItemRequest(
            UUID workerId,
            @NotBlank(message = "La descripcion es obligatoria")
            @Size(max = 500, message = "La descripcion no puede exceder 500 caracteres")
            String description,
            BigDecimal quantity,
            String unit,
            BigDecimal unitCost
    ) {}

    public record UpdateItemRequest(
            UUID workerId,
            @NotBlank(message = "La descripcion es obligatoria")
            @Size(max = 500, message = "La descripcion no puede exceder 500 caracteres")
            String description,
            BigDecimal quantity,
            String unit,
            BigDecimal unitCost
    ) {}

    public record CancelRequest(
            @NotBlank(message = "El motivo de cancelación es obligatorio") String reason
    ) {}

    public record ItemResponse(
            UUID id, UUID workerId, String description,
            BigDecimal quantity, String unit, BigDecimal unitCost,
            BigDecimal total, int position
    ) {
        public static ItemResponse from(RequestItem i) {
            return new ItemResponse(i.getId(), i.getWorkerId(), i.getDescription(),
                    i.getQuantity(), i.getUnit(), i.getUnitCost(), i.getTotal(), i.getPosition());
        }
    }

    public record Response(
            UUID id,
            Long folio,
            String folioFormatted,
            RequestStatus status,
            UUID supplierId,
            UUID solicitanteId,
            String destination,
            String authorizer,
            UUID createdBy,
            List<ItemResponse> items,
            Instant createdAt,
            Instant issuedAt,
            Instant cancelledAt,
            String cancellationReason
    ) {
        public static Response from(Request r) {
            String formatted = r.getFolio() != null
                    ? String.format("%07d", r.getFolio())
                    : null;

            return new Response(
                    r.getId(), r.getFolio(), formatted, r.getStatus(),
                    r.getSupplierId(), r.getSolicitanteId(),
                    r.getDestination(), r.getAuthorizer(), r.getCreatedBy(),
                    r.getItems().stream().map(ItemResponse::from).toList(),
                    r.getCreatedAt(), r.getIssuedAt(),
                    r.getCancelledAt(), r.getCancellationReason()
            );
        }
    }

    public record StatsResponse(
            long borradores,
            long emitidas,
            long canceladas,
            List<Response> recent
    ) {}
}

package mx.juarezdeoriente.solicitudes.requests.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Crea una solicitud con sus artículos y la emite en una sola operación. */
public record CreateAndIssueRequest(
        @NotNull(message = "El proveedor es obligatorio") UUID supplierId,
        UUID solicitanteId,
        @NotBlank(message = "El destino es obligatorio")
        @Size(max = 1000) String destination,
        String authorizer,
        @NotEmpty(message = "Debe agregar al menos un artículo") List<AddItemRequest> items
) {}

package mx.juarezdeoriente.solicitudes.requests.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDraftRequest(
        @NotNull(message = "El proveedor es obligatorio") UUID supplierId,
        @NotBlank(message = "El destino/proposito es obligatorio") @Size(max = 1000, message = "El destino no puede exceder 1000 caracteres") String destination,
        String authorizer
) {}

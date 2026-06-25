package mx.juarezdeoriente.solicitudes.requests.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record AddItemRequest(
        UUID workerId,
        @NotBlank(message = "La descripcion es obligatoria") @Size(max = 500, message = "La descripcion no puede exceder 500 caracteres") String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitCost
) {}

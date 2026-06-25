package mx.juarezdeoriente.solicitudes.requests.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelRequest(
        @NotBlank(message = "El motivo de cancelación es obligatorio") String reason
) {}

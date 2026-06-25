package mx.juarezdeoriente.solicitudes.workers.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import mx.juarezdeoriente.solicitudes.workers.domain.model.WorkerType;

public record WorkerRequest(
        String companyCode,
        String employeeNumber,
        @NotBlank(message = "El nombre del trabajador es obligatorio") @Size(max = 200, message = "El nombre no puede exceder 200 caracteres") String name,
        String phone,
        @NotNull(message = "El tipo de trabajador es obligatorio") WorkerType workerType
) {}

package mx.juarezdeoriente.solicitudes.workers.dto;
import mx.juarezdeoriente.solicitudes.workers.WorkerType;
import mx.juarezdeoriente.solicitudes.workers.Worker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class WorkerDto {

    public record CreateRequest(
            String companyCode,
            String employeeNumber,
            @NotBlank(message = "El nombre del trabajador es obligatorio")
            @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
            String name,
            String phone,
            @NotNull(message = "El tipo de trabajador es obligatorio") WorkerType workerType
    ) {}

    public record UpdateRequest(
            String companyCode,
            String employeeNumber,
            String name,
            String phone,
            WorkerType workerType,
            Boolean active
    ) {}

    public record Response(
            UUID id, String companyCode, String employeeNumber,
            String name, String phone, WorkerType workerType,
            boolean active, Instant createdAt
    ) {
        public static Response from(Worker w) {
            return new Response(w.getId(), w.getCompanyCode(), w.getEmployeeNumber(),
                    w.getName(), w.getPhone(), w.getWorkerType(), w.isActive(), w.getCreatedAt());
        }
    }
}

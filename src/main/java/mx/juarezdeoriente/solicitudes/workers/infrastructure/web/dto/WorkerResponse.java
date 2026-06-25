package mx.juarezdeoriente.solicitudes.workers.infrastructure.web.dto;

import mx.juarezdeoriente.solicitudes.workers.domain.model.Worker;
import mx.juarezdeoriente.solicitudes.workers.domain.model.WorkerType;

import java.time.Instant;
import java.util.UUID;

public record WorkerResponse(
        UUID id, String companyCode, String employeeNumber,
        String name, String phone, WorkerType workerType,
        boolean active, Instant createdAt
) {
    public static WorkerResponse from(Worker w) {
        return new WorkerResponse(w.getId(), w.getCompanyCode(), w.getEmployeeNumber(),
                w.getName(), w.getPhone(), w.getWorkerType(), w.isActive(), w.getCreatedAt());
    }
}

package mx.juarezdeoriente.solicitudes.workers.infrastructure.web.dto;

import mx.juarezdeoriente.solicitudes.workers.domain.model.WorkerType;

public record WorkerUpdateRequest(
        String companyCode, String employeeNumber,
        String name, String phone, WorkerType workerType, Boolean active
) {}

package mx.juarezdeoriente.solicitudes.suppliers.infrastructure.web.dto;

import mx.juarezdeoriente.solicitudes.suppliers.domain.model.Supplier;

import java.time.Instant;
import java.util.UUID;

public record SupplierResponse(
        UUID id, String code, String name, String phone,
        boolean active, Instant createdAt
) {
    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(s.getId(), s.getCode(), s.getName(),
                s.getPhone(), s.isActive(), s.getCreatedAt());
    }
}

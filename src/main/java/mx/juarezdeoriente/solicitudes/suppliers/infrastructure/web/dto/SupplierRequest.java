package mx.juarezdeoriente.solicitudes.suppliers.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank(message = "La clave del proveedor es obligatoria") @Size(max = 50, message = "La clave no puede exceder 50 caracteres") String code,
        @NotBlank(message = "El nombre del proveedor es obligatorio") @Size(max = 300, message = "El nombre no puede exceder 300 caracteres") String name,
        String phone
) {}

package mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
        @NotBlank @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres") String newPassword
) {}

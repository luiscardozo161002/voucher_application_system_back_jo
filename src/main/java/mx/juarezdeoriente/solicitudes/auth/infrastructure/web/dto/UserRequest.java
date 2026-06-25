package mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;

import java.util.Set;

public record UserRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio") String username,
        @NotBlank(message = "La contraseña es obligatoria") String password,
        @NotBlank(message = "El nombre completo es obligatorio") String displayName,
        String phone,
        @NotEmpty(message = "Debe asignar al menos un rol") Set<Role> roles
) {}

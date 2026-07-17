package mx.juarezdeoriente.solicitudes.modules.users.application.dto;
import mx.juarezdeoriente.solicitudes.modules.users.domain.Role;
import mx.juarezdeoriente.solicitudes.modules.users.domain.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class UserDto {

    public record LoginRequest(
            @NotBlank(message = "El usuario es obligatorio") String username,
            @NotBlank(message = "La contraseña es obligatoria") String password
    ) {}

    public record LoginResponse(
            String token,
            String tokenType,
            long expiresIn,
            UserInfo user
    ) {
        public record UserInfo(
                UUID id,
                String username,
                String displayName,
                Set<Role> roles
        ) {}
    }

    public record CreateRequest(
            @NotBlank(message = "El nombre de usuario es obligatorio") String username,
            @NotBlank(message = "La contraseña es obligatoria") String password,
            @NotBlank(message = "El nombre completo es obligatorio") String displayName,
            String phone,
            @NotEmpty(message = "Debe asignar al menos un rol") Set<Role> roles
    ) {}

    public record UpdateRequest(
            String displayName,
            String phone,
            Set<Role> roles,
            Boolean active
    ) {}

    public record Response(
            UUID id,
            String username,
            String displayName,
            String phone,
            Set<Role> roles,
            boolean active,
            boolean requiresPasswordReset,
            Instant createdAt
    ) {
        public static Response from(User user) {
            return new Response(
                    user.getId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    user.getPhone(),
                    user.getRoles(),
                    user.isActive(),
                    user.isRequiresPasswordReset(),
                    user.getCreatedAt()
            );
        }
    }

    public record RefreshRequest(String refreshToken) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
            @NotBlank @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres") String newPassword
    ) {}
}

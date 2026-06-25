package mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto;

import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;
import mx.juarezdeoriente.solicitudes.auth.domain.model.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String displayName,
        String phone,
        Set<Role> roles,
        boolean active,
        boolean requiresPasswordReset,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
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

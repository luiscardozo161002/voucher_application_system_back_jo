package mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto;

import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;

import java.util.Set;
import java.util.UUID;

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

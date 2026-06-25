package mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto;

import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;

import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        String token,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn,
        UserInfo user
) {
    public record UserInfo(
            UUID id,
            String username,
            String displayName,
            Set<Role> roles
    ) {}
}

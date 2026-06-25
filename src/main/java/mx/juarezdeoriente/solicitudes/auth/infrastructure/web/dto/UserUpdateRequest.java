package mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto;

import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;

import java.util.Set;

public record UserUpdateRequest(
        String displayName,
        String phone,
        Set<Role> roles,
        Boolean active
) {}

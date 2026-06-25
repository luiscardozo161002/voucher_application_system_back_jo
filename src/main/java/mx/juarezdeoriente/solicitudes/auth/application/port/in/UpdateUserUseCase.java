package mx.juarezdeoriente.solicitudes.auth.application.port.in;

import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;
import mx.juarezdeoriente.solicitudes.auth.domain.model.User;

import java.util.Set;
import java.util.UUID;

public interface UpdateUserUseCase {

    record Command(UUID userId, String displayName, String phone, Set<Role> roles, Boolean active) {}

    User execute(Command command);
}

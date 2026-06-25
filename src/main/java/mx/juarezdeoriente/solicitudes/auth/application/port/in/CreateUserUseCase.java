package mx.juarezdeoriente.solicitudes.auth.application.port.in;

import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;
import mx.juarezdeoriente.solicitudes.auth.domain.model.User;

import java.util.Set;

public interface CreateUserUseCase {

    record Command(
            String username,
            String password,
            String displayName,
            String phone,
            Set<Role> roles
    ) {}

    User execute(Command command);
}

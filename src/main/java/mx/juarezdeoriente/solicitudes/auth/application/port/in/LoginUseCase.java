package mx.juarezdeoriente.solicitudes.auth.application.port.in;

import mx.juarezdeoriente.solicitudes.auth.domain.model.User;

public interface LoginUseCase {

    record Command(String username, String password) {}

    /** Valida credenciales y retorna el usuario autenticado. */
    User execute(Command command);
}

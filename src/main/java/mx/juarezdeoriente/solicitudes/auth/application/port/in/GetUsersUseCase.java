package mx.juarezdeoriente.solicitudes.auth.application.port.in;

import mx.juarezdeoriente.solicitudes.auth.domain.model.User;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;

import java.util.UUID;

public interface GetUsersUseCase {

    User findById(UUID id);

    PageResult<User> findAll(int page, int size);
}

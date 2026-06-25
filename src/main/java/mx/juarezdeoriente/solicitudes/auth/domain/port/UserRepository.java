package mx.juarezdeoriente.solicitudes.auth.domain.port;

import mx.juarezdeoriente.solicitudes.auth.domain.model.User;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida (output port) del módulo auth. */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    PageResult<User> findAll(int page, int size);
}

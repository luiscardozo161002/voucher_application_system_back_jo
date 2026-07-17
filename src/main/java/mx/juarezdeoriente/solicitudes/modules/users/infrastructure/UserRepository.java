package mx.juarezdeoriente.solicitudes.modules.users.infrastructure;
import mx.juarezdeoriente.solicitudes.modules.users.domain.Role;
import mx.juarezdeoriente.solicitudes.modules.users.domain.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRolesContaining(String role);

    long countByRolesContainingAndActive(String role, boolean active);

    Page<User> findAll(Pageable pageable);
}

package mx.juarezdeoriente.solicitudes.auth;
import mx.juarezdeoriente.solicitudes.auth.Role;
import mx.juarezdeoriente.solicitudes.auth.User;

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

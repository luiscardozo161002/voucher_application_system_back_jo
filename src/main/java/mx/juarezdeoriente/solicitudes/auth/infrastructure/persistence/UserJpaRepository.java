package mx.juarezdeoriente.solicitudes.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}

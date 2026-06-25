package mx.juarezdeoriente.solicitudes.shared.infrastructure.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyJpaEntity, UUID> {

    Optional<IdempotencyKeyJpaEntity> findByKeyHash(String keyHash);

    @Modifying
    @Query("DELETE FROM IdempotencyKeyJpaEntity k WHERE k.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpired();
}

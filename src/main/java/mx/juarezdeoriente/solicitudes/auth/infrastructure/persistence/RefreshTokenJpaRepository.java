package mx.juarezdeoriente.solicitudes.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revoked = true WHERE r.userId = :userId")
    void revokeAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM RefreshTokenJpaEntity r WHERE r.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpired();
}

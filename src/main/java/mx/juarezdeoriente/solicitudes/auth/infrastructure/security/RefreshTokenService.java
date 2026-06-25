package mx.juarezdeoriente.solicitudes.auth.infrastructure.security;

import mx.juarezdeoriente.solicitudes.auth.infrastructure.persistence.RefreshTokenJpaEntity;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.persistence.RefreshTokenJpaRepository;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Gestiona el ciclo de vida de los refresh tokens en base de datos.
 * Solo se guarda el hash SHA-256 del token — nunca el token en claro.
 */
@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenJpaRepository repository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    /** Persiste el hash del refresh token emitido en login. */
    public void store(UUID userId, String rawToken) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        entity.setCreatedAt(Instant.now());
        entity.setRevoked(false);
        repository.save(entity);
    }

    /**
     * Valida que el token existe en BD, no está revocado y no expiró.
     * Luego lo revoca (rotación: un token solo se usa una vez).
     */
    public UUID validateAndRotate(String rawToken) {
        String tokenHash = hash(rawToken);

        RefreshTokenJpaEntity entity = repository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new DomainException(
                        "El refresh token es invalido o ya fue utilizado. Inicia sesion nuevamente."));

        if (entity.isRevoked()) {
            // Posible robo de token — revocar todos los del usuario como precaución
            repository.revokeAllByUserId(entity.getUserId());
            throw new DomainException(
                    "El refresh token ya fue utilizado. Por seguridad se cerraron todas las sesiones. Inicia sesion nuevamente.");
        }

        if (Instant.now().isAfter(entity.getExpiresAt())) {
            throw new DomainException(
                    "El refresh token ha expirado. Inicia sesion nuevamente.");
        }

        // Rotación: revocar el token actual antes de emitir uno nuevo
        entity.setRevoked(true);
        repository.save(entity);

        return entity.getUserId();
    }

    /** Revoca todos los refresh tokens del usuario (logout completo). */
    public void revokeAll(UUID userId) {
        repository.revokeAllByUserId(userId);
    }

    /** Limpieza automática de tokens expirados — se ejecuta diariamente. */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpired() {
        repository.deleteExpired();
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }
}

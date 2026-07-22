package mx.juarezdeoriente.solicitudes.shared.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementa el patrón de idempotency keys para peticiones POST mutantes.
 *
 * Uso del cliente:
 *   POST /api/v1/requests
 *   Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
 *
 * Si la misma clave se envía dos veces, el servidor retorna la respuesta
 * almacenada sin ejecutar la operación de nuevo — el folio no se duplica.
 *
 * La clave expira en 24 horas.
 */
@Component
@ConditionalOnBean(IdempotencyKeyJpaRepository.class)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    static final String HEADER = "Idempotency-Key";

    private final IdempotencyKeyJpaRepository repository;

    public IdempotencyFilter(IdempotencyKeyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String rawKey = request.getHeader(HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String userId  = extractUserId(request);
        String keyHash = hash(userId + ":" + rawKey);

        Optional<IdempotencyKeyJpaEntity> existing = repository.findByKeyHash(keyHash);

        if (existing.isPresent()) {
            IdempotencyKeyJpaEntity stored = existing.get();
            log.debug("Idempotency hit para key={} → status={}", rawKey, stored.getHttpStatus());

            // Devolver respuesta almacenada sin re-ejecutar la operación
            response.setStatus(stored.getHttpStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.addHeader("X-Idempotency-Replayed", "true");
            response.getWriter().write(stored.getResponse());
            return;
        }

        // Capturar la respuesta para poder almacenarla
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrapper);

        String responseBody = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        int    status       = wrapper.getStatus();

        // Solo guardar respuestas exitosas (2xx) para evitar cachear errores transitorios
        if (status >= 200 && status < 300) {
            saveIdempotencyRecord(keyHash, request.getRequestURI(), status, responseBody);
        }

        wrapper.copyBodyToResponse();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return true;
        String uri = request.getRequestURI();
        // Solo aplica a la creación de solicitudes (asigna folio único irreversible)
        return !uri.matches(".*/api/v1/requests/?$");
    }

    @Transactional
    protected void saveIdempotencyRecord(String keyHash, String endpoint,
                                         int status, String responseBody) {
        IdempotencyKeyJpaEntity entity = new IdempotencyKeyJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setKeyHash(keyHash);
        entity.setEndpoint(endpoint);
        entity.setHttpStatus(status);
        entity.setResponse(responseBody);
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(Instant.now().plusSeconds(86400)); // 24h
        repository.save(entity);
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void cleanupExpired() {
        repository.deleteExpired();
        log.debug("Limpieza de idempotency keys expiradas completada");
    }

    private String extractUserId(HttpServletRequest request) {
        // Usar IP como fallback si no hay usuario autenticado (ej. pre-auth)
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }
}

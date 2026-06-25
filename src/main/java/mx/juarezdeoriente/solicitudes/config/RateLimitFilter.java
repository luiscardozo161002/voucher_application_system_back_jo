package mx.juarezdeoriente.solicitudes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.juarezdeoriente.solicitudes.shared.infrastructure.web.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting por IP usando Bucket4j (token bucket algorithm).
 *
 * Límites aplicados:
 * - /auth/login   → 5 intentos / 1 minuto  (protección contra fuerza bruta)
 * - /auth/refresh → 10 intentos / 1 minuto
 * - Resto de POST → 60 peticiones / 1 minuto
 *
 * En producción multi-instancia reemplazar el ConcurrentHashMap por Bucket4j + Redis.
 */
@Component
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Map<String, Bucket> loginBuckets   = new ConcurrentHashMap<>();
    private final Map<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper        mapper;

    public RateLimitFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String ip  = extractIp(request);
        String uri = request.getRequestURI();

        Bucket bucket;
        if (uri.endsWith("/auth/login")) {
            bucket = loginBuckets.computeIfAbsent(ip, k -> buildBucket(5, Duration.ofMinutes(1)));
        } else if (uri.endsWith("/auth/refresh")) {
            bucket = refreshBuckets.computeIfAbsent(ip, k -> buildBucket(10, Duration.ofMinutes(1)));
        } else {
            bucket = generalBuckets.computeIfAbsent(ip, k -> buildBucket(60, Duration.ofMinutes(1)));
        }

        if (bucket.tryConsume(1)) {
            long remaining = bucket.getAvailableTokens();
            response.addHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit excedido para IP={} en URI={}", ip, uri);
            rejectWithTooManyRequests(response, uri);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/api-docs")
                || "GET".equals(request.getMethod());
    }

    private Bucket buildBucket(int tokens, Duration period) {
        Bandwidth limit = Bandwidth.classic(tokens, Refill.greedy(tokens, period));
        return Bucket.builder().addLimit(limit).build();
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void rejectWithTooManyRequests(HttpServletResponse response, String uri) throws IOException {
        String mensaje = uri.contains("/auth/login")
                ? "Demasiados intentos de inicio de sesion. Espera 1 minuto e intenta nuevamente."
                : "Demasiadas peticiones. Por favor espera un momento.";

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Retry-After", "60");
        mapper.writeValue(response.getWriter(), ApiResponse.error(mensaje));
    }
}

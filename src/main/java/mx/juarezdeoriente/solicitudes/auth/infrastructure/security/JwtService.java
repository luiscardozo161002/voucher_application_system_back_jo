package mx.juarezdeoriente.solicitudes.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String INSECURE_DEFAULT_SECRET =
            "c29saWNpdHVkZXMtc2VjcmV0LWp1YXJlei1kZS1vcmllbnRlLTIwMjYtc2VjdXJl";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${app.jwt.issuer:solicitudes-api}")
    private String issuer;

    @Value("${app.jwt.audience:solicitudes-client}")
    private String audience;

    @PostConstruct
    void warnIfInsecureSecret() {
        if (INSECURE_DEFAULT_SECRET.equals(secret)) {
            log.warn("=================================================================");
            log.warn("ADVERTENCIA DE SEGURIDAD: Estas usando el secreto JWT por defecto.");
            log.warn("En produccion define JWT_SECRET con un valor aleatorio de 256+ bits.");
            log.warn("=================================================================");
        }
    }

    public String generateToken(AppUserDetails user) {
        List<String> roles = user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())       // jti: ID único → permite auditoría
                .issuer(issuer)                          // iss: previene token confusion
                .audience().add(audience).and()          // aud: limita el ámbito del token
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("roles", roles)
                .claim("tv", user.getTokenVersion())     // tv: token version para invalidación
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    public String generateRefreshToken(AppUserDetails user) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("tv", user.getTokenVersion())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get("userId", String.class));
    }

    public int extractTokenVersion(String token) {
        Integer tv = parseClaims(token).get("tv", Integer.class);
        return tv != null ? tv : 0;
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(parseClaims(token).get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationMs()        { return expirationMs; }
    public long getRefreshExpirationMs() { return refreshExpirationMs; }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}

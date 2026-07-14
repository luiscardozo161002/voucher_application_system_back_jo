package mx.juarezdeoriente.solicitudes.auth;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean revoked;

    public RefreshToken() {}

    public UUID getId()                 { return id; }
    public void setId(UUID v)           { this.id = v; }
    public UUID getUserId()             { return userId; }
    public void setUserId(UUID v)       { this.userId = v; }
    public String getTokenHash()        { return tokenHash; }
    public void setTokenHash(String v)  { this.tokenHash = v; }
    public Instant getExpiresAt()       { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }
    public Instant getCreatedAt()       { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public boolean isRevoked()          { return revoked; }
    public void setRevoked(boolean v)   { this.revoked = v; }
}

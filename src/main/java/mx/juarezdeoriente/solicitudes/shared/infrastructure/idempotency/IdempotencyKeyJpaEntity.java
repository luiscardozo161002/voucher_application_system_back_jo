package mx.juarezdeoriente.solicitudes.shared.infrastructure.idempotency;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
class IdempotencyKeyJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "key_hash", nullable = false, length = 64, unique = true)
    private String keyHash;

    @Column(nullable = false, length = 200)
    private String endpoint;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(nullable = false, columnDefinition = "text")
    private String response;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyKeyJpaEntity() {}

    public UUID getId()                 { return id; }
    public void setId(UUID v)           { this.id = v; }
    public String getKeyHash()          { return keyHash; }
    public void setKeyHash(String v)    { this.keyHash = v; }
    public String getEndpoint()         { return endpoint; }
    public void setEndpoint(String v)   { this.endpoint = v; }
    public int getHttpStatus()          { return httpStatus; }
    public void setHttpStatus(int v)    { this.httpStatus = v; }
    public String getResponse()         { return response; }
    public void setResponse(String v)   { this.response = v; }
    public Instant getCreatedAt()       { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getExpiresAt()       { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }
}

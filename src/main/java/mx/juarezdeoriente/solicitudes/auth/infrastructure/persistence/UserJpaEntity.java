package mx.juarezdeoriente.solicitudes.auth.infrastructure.persistence;

import jakarta.persistence.*;
import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(length = 20)
    private String phone;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "requires_password_reset", nullable = false)
    private boolean requiresPasswordReset;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Version
    private Long version;


    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected UserJpaEntity() {}

    // Getters y setters

    public UUID getId()                        { return id; }
    public void setId(UUID id)                 { this.id = id; }

    public String getUsername()                { return username; }
    public void setUsername(String v)          { this.username = v; }

    public String getPasswordHash()            { return passwordHash; }
    public void setPasswordHash(String v)      { this.passwordHash = v; }

    public String getDisplayName()             { return displayName; }
    public void setDisplayName(String v)       { this.displayName = v; }

    public String getPhone()                   { return phone; }
    public void setPhone(String v)             { this.phone = v; }

    public Set<Role> getRoles()                { return roles; }
    public void setRoles(Set<Role> v)          { this.roles = v; }

    public boolean isActive()                  { return active; }
    public void setActive(boolean v)           { this.active = v; }

    public boolean isRequiresPasswordReset()   { return requiresPasswordReset; }
    public void setRequiresPasswordReset(boolean v) { this.requiresPasswordReset = v; }

    public int getFailedLoginAttempts()        { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int v)  { this.failedLoginAttempts = v; }

    public Instant getLockedUntil()             { return lockedUntil; }
    public void setLockedUntil(Instant v)       { this.lockedUntil = v; }

    public int getTokenVersion()                { return tokenVersion; }
    public void setTokenVersion(int v)          { this.tokenVersion = v; }

    public Instant getCreatedAt()               { return createdAt; }
    public void setCreatedAt(Instant v)         { this.createdAt = v; }
    public Instant getUpdatedAt()               { return updatedAt; }
    public void setUpdatedAt(Instant v)         { this.updatedAt = v; }
    public Long getVersion()                    { return version; }
    public void setVersion(Long v)              { this.version = v; }
}

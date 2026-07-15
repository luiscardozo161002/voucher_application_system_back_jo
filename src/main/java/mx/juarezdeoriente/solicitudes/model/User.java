package mx.juarezdeoriente.solicitudes.model;
import mx.juarezdeoriente.solicitudes.model.Role;

import jakarta.persistence.*;
import mx.juarezdeoriente.solicitudes.exception.DomainException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

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

    @ElementCollection(fetch = FetchType.EAGER)
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

    protected User() {}

    public static User create(String username, String passwordHash,
                              String displayName, String phone, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new DomainException("El usuario debe tener al menos un rol");
        }
        User user = new User();
        user.id                    = UUID.randomUUID();
        user.username              = username;
        user.passwordHash          = passwordHash;
        user.displayName           = displayName;
        user.phone                 = phone;
        user.roles                 = EnumSet.copyOf(roles);
        user.active                = true;
        user.requiresPasswordReset = false;
        user.failedLoginAttempts   = 0;
        user.lockedUntil           = null;
        user.tokenVersion          = 0;
        user.createdAt             = Instant.now();
        user.updatedAt             = user.createdAt;
        return user;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash          = newPasswordHash;
        this.requiresPasswordReset = false;
        this.failedLoginAttempts   = 0;
        this.lockedUntil           = null;
        this.tokenVersion++;
        this.updatedAt             = Instant.now();
    }

    public void recordFailedLogin(int maxAttempts, int lockoutMinutes) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = Instant.now().plusSeconds(lockoutMinutes * 60L);
        }
        this.updatedAt = Instant.now();
    }

    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil         = null;
        this.updatedAt           = Instant.now();
    }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public void activate() {
        this.active    = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.tokenVersion++;
        this.updatedAt = Instant.now();
    }

    public void update(String displayName, String phone, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new DomainException("El usuario debe tener al menos un rol");
        }
        this.displayName = displayName;
        this.phone       = phone;
        this.roles.clear();
        this.roles.addAll(roles);
        this.updatedAt   = Instant.now();
    }

    public UUID getId()                      { return id; }
    public String getUsername()              { return username; }
    public String getPasswordHash()          { return passwordHash; }
    public String getDisplayName()           { return displayName; }
    public String getPhone()                 { return phone; }
    public Set<Role> getRoles()              { return EnumSet.copyOf(roles); }
    public boolean isActive()               { return active; }
    public boolean isRequiresPasswordReset() { return requiresPasswordReset; }
    public int getFailedLoginAttempts()      { return failedLoginAttempts; }
    public Instant getLockedUntil()          { return lockedUntil; }
    public Instant getCreatedAt()            { return createdAt; }
    public int getTokenVersion()             { return tokenVersion; }
}

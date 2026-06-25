package mx.juarezdeoriente.solicitudes.auth.domain.model;

import mx.juarezdeoriente.solicitudes.auth.domain.event.UserCreatedEvent;
import mx.juarezdeoriente.solicitudes.auth.domain.event.UserDeactivatedEvent;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.domain.model.AggregateRoot;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class User extends AggregateRoot {

    private final UUID id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String phone;
    private final Set<Role> roles;
    private boolean active;
    private boolean requiresPasswordReset;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private final Instant createdAt;

    /**
     * Versión del token. Al incrementarse, todos los JWT emitidos anteriormente
     * quedan inválidos de forma inmediata sin necesidad de una blacklist.
     * Se incrementa en: cambio de contraseña y desactivación de cuenta.
     */
    private int tokenVersion;

    private User(UUID id, String username, String passwordHash,
                 String displayName, String phone, Set<Role> roles,
                 boolean requiresPasswordReset, Instant createdAt) {
        this.id                    = id;
        this.username              = username;
        this.passwordHash          = passwordHash;
        this.displayName           = displayName;
        this.phone                 = phone;
        this.roles                 = EnumSet.copyOf(roles);
        this.active                = true;
        this.requiresPasswordReset = requiresPasswordReset;
        this.failedLoginAttempts   = 0;
        this.lockedUntil           = null;
        this.createdAt             = createdAt;
        this.tokenVersion          = 0;
    }

    public static User reconstitute(UUID id, String username, String passwordHash,
                                    String displayName, String phone, Set<Role> roles,
                                    boolean active, boolean requiresPasswordReset,
                                    int failedLoginAttempts, Instant lockedUntil,
                                    Instant createdAt, int tokenVersion) {
        User user = new User(id, username, passwordHash, displayName, phone, roles,
                requiresPasswordReset, createdAt);
        user.active              = active;
        user.failedLoginAttempts = failedLoginAttempts;
        user.lockedUntil         = lockedUntil;
        user.tokenVersion        = tokenVersion;
        return user;
    }

    public static User create(String username, String passwordHash,
                              String displayName, String phone, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new DomainException("El usuario debe tener al menos un rol");
        }
        User user = new User(UUID.randomUUID(), username, passwordHash,
                displayName, phone, roles, false, Instant.now());
        user.registerEvent(new UserCreatedEvent(user.id, username));
        return user;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash          = newPasswordHash;
        this.requiresPasswordReset = false;
        this.failedLoginAttempts   = 0;
        this.lockedUntil           = null;
        this.tokenVersion++;   // invalida todos los tokens anteriores
    }

    public void recordFailedLogin(int maxAttempts, int lockoutMinutes) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = Instant.now().plusSeconds(lockoutMinutes * 60L);
        }
    }

    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil         = null;
    }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public void deactivate() {
        this.active = false;
        this.tokenVersion++;   // invalida todos los tokens anteriores
        registerEvent(new UserDeactivatedEvent(this.id, this.username));
    }

    public void activate() {
        this.active = true;
    }

    public void update(String displayName, String phone, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new DomainException("El usuario debe tener al menos un rol");
        }
        this.displayName = displayName;
        this.phone       = phone;
        this.roles.clear();
        this.roles.addAll(roles);
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

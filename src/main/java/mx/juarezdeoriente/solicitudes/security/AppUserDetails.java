package mx.juarezdeoriente.solicitudes.security;
import mx.juarezdeoriente.solicitudes.modules.users.domain.Role;

import mx.juarezdeoriente.solicitudes.modules.users.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * Principal personalizado que expone el UUID del usuario.
 * Los controllers lo reciben vía @AuthenticationPrincipal AppUserDetails.
 */
public class AppUserDetails implements UserDetails {

    private final UUID   id;
    private final String username;
    private final String passwordHash;
    private final Collection<GrantedAuthority> authorities;
    private final boolean active;
    private final boolean locked;
    private final int tokenVersion;

    public AppUserDetails(User user) {
        this.id           = user.getId();
        this.username     = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.active       = user.isActive();
        this.locked       = user.isLocked();
        this.tokenVersion = user.getTokenVersion();
        this.authorities  = user.getRoles().stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.name()))
                .toList();
    }

    public UUID getId()           { return id; }
    public int getTokenVersion()  { return tokenVersion; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()   { return passwordHash; }
    @Override public String getUsername()   { return username; }
    @Override public boolean isEnabled()    { return active; }
    @Override public boolean isAccountNonLocked() { return !locked; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}

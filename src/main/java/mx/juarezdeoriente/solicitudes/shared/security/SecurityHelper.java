package mx.juarezdeoriente.solicitudes.shared.security;

import mx.juarezdeoriente.solicitudes.auth.security.AppUserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("security")
public class SecurityHelper {

    public static final String ACCESS_DENIED = "No tienes permiso para realizar esta acción";

    /**
     * Devuelve true si el usuario SOLO tiene rol CAPTURISTA
     * (no es ADMIN ni AUDITOR). Usado para filtrar recursos por propietario.
     */
    public boolean isCapturistaOnly(AppUserDetails user) {
        return user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .noneMatch(r -> r.equals(AppRoles.ROLE_ADMIN) || r.equals(AppRoles.ROLE_AUDITOR));
    }

    /**
     * Devuelve el ID a usar como filtro de propietario:
     *  - CAPTURISTA: su propio ID
     *  - ADMIN / AUDITOR: null (sin filtro)
     */
    public UUID ownerFilter(AppUserDetails user) {
        return isCapturistaOnly(user) ? user.getId() : null;
    }
}

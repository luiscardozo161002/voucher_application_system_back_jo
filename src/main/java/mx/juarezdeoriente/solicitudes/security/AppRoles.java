package mx.juarezdeoriente.solicitudes.security;

/** Constantes de roles de Spring Security. Evita strings mágicos dispersos. */
public final class AppRoles {

    private AppRoles() {}

    // Prefijo que Spring Security agrega automáticamente
    public static final String ROLE_ADMIN       = "ROLE_ADMIN";
    public static final String ROLE_CAPTURISTA  = "ROLE_CAPTURISTA";
    public static final String ROLE_AUDITOR     = "ROLE_AUDITOR";
    public static final String ROLE_AUTORIZADOR = "ROLE_AUTORIZADOR";

    // Sin prefijo — para usar en @PreAuthorize("hasAnyRole(...)")
    public static final String ADMIN       = "ADMIN";
    public static final String CAPTURISTA  = "CAPTURISTA";
    public static final String AUDITOR     = "AUDITOR";
    public static final String AUTORIZADOR = "AUTORIZADOR";
}

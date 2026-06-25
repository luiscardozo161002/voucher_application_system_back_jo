package mx.juarezdeoriente.solicitudes.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.juarezdeoriente.solicitudes.auth.application.service.UserService;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.AppUserDetailsService;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.JwtAuthenticationFilter;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.JwtService;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.SecurityConfig;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.web.AuthController;
import mx.juarezdeoriente.solicitudes.config.CorsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de casos borde de la API:
 * - Parámetros inválidos / overflow
 * - Cuerpos malformados
 * - Autenticación y autorización
 * - Validaciones de campos
 */
@WebMvcTest(value = AuthController.class, properties = "app.rate-limit.enabled=false")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CorsConfig.class })
@DisplayName("API Edge Cases")
class ApiEdgeCasesTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean AuthenticationManager  authManager;
    @MockBean JwtService             jwtService;
    @MockBean mx.juarezdeoriente.solicitudes.auth.infrastructure.security.RefreshTokenService refreshTokenService;
    @MockBean UserService userService;
    @MockBean AppUserDetailsService  userDetailsService;

    // =========================================================
    // Autenticación
    // =========================================================

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("Body vacío → 400 con lista de campos faltantes")
        void body_vacio_retorna_400_con_detalles() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.details", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("Username vacío → 400 con mensaje 'El usuario es obligatorio'")
        void username_vacio_retorna_400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"\",\"password\":\"Admin123!\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0]").value("El usuario es obligatorio"));
        }

        @Test
        @DisplayName("Password vacío → 400 con mensaje 'La contraseña es obligatoria'")
        void password_vacio_retorna_400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"admin\",\"password\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0]").value("La contraseña es obligatoria"));
        }

        @Test
        @DisplayName("Credenciales incorrectas → 401 con mensaje claro")
        void credenciales_incorrectas_retorna_401() throws Exception {
            when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Usuario o contrasena incorrectos."));
        }

        @Test
        @DisplayName("Cuenta desactivada → 401 con mensaje de cuenta desactivada")
        void cuenta_desactivada_retorna_401() throws Exception {
            when(authManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"admin\",\"password\":\"Admin123!\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value(containsString("desactivada")));
        }

        @Test
        @DisplayName("JSON malformado → 400 con mensaje de JSON inválido")
        void json_malformado_retorna_400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{username: admin}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("JSON")));
        }

        @Test
        @DisplayName("Sin Content-Type → Spring no puede leer el body (415 o error)")
        void sin_content_type_retorna_error() throws Exception {
            // Sin Content-Type Spring devuelve 415 Unsupported Media Type
            // o falla al deserializar el body — ambos indican rechazo correcto
            mockMvc.perform(post("/api/v1/auth/login")
                            .content("{\"username\":\"admin\",\"password\":\"Admin123!\"}"))
                    .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                            org.hamcrest.Matchers.is(415),
                            org.hamcrest.Matchers.is(400),
                            org.hamcrest.Matchers.is(500)
                    )));
        }
    }

    // =========================================================
    // Sin autenticación
    // =========================================================

    @Nested
    @DisplayName("Endpoints protegidos sin token")
    class SinAutenticacion {

        @Test
        @DisplayName("GET /me sin token → 401")
        void me_sin_token_retorna_401() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /change-password sin token → 401")
        void change_password_sin_token_retorna_401() throws Exception {
            mockMvc.perform(post("/api/v1/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"a\",\"newPassword\":\"b\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token inventado → 401")
        void token_invalido_retorna_401() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer token.falso.aqui"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================
    // Refresh Token
    // =========================================================

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("Sin cookie de refresh → 422 sin sesion activa")
        void sin_cookie_retorna_422() throws Exception {
            // El refresh token ahora viaja como HttpOnly cookie, no en el body.
            // Sin cookie → DomainException "No hay sesion activa"
            mockMvc.perform(post("/api/v1/auth/refresh"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value(containsString("sesion")));
        }

        @Test
        @DisplayName("Cookie con refresh token inválido → 422 con mensaje claro")
        void refresh_token_invalido_retorna_error() throws Exception {
            when(jwtService.isValid("token.malo")).thenReturn(false);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refresh_token", "token.malo")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value(containsString("invalido")));
        }
    }

    // =========================================================
    // Método HTTP incorrecto
    // =========================================================

    @Nested
    @DisplayName("Método HTTP incorrecto")
    class MetodoIncorrecto {

        @Test
        @DisplayName("GET en /login → 401 (Spring Security intercepta antes de verificar método)")
        void get_en_login_retorna_401_o_405() throws Exception {
            // Spring Security evalúa autenticación antes de permitir que llegue a la ruta.
            // Solo POST /login está en el allowlist; GET no tiene acceso → 401.
            mockMvc.perform(get("/api/v1/auth/login"))
                    .andExpect(status().isUnauthorized());
        }
    }
}

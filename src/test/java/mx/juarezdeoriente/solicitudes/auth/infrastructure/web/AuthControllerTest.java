package mx.juarezdeoriente.solicitudes.auth.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.juarezdeoriente.solicitudes.auth.application.port.in.ChangePasswordUseCase;
import mx.juarezdeoriente.solicitudes.auth.application.port.in.GetUsersUseCase;
import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;
import mx.juarezdeoriente.solicitudes.auth.domain.model.User;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.AppUserDetails;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.AppUserDetailsService;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.JwtAuthenticationFilter;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.JwtService;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.SecurityConfig;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto.LoginRequest;
import mx.juarezdeoriente.solicitudes.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, properties = "app.rate-limit.enabled=false")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CorsConfig.class })
class AuthControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean AuthenticationManager   authManager;
    @MockBean JwtService              jwtService;
    @MockBean mx.juarezdeoriente.solicitudes.auth.infrastructure.security.RefreshTokenService refreshTokenService;
    @MockBean ChangePasswordUseCase   changePasswordUseCase;
    @MockBean GetUsersUseCase         getUsersUseCase;
    @MockBean AppUserDetailsService  userDetailsService;

    @Test
    void login_con_credenciales_validas_retorna_200_y_token() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.reconstitute(userId, "admin", "hash", "Administrador", null, Set.of(Role.ADMIN), true, false, 0, null, Instant.now(), 0);
        AppUserDetails details = new AppUserDetails(user);

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        when(jwtService.generateToken(any())).thenReturn("token.jwt.aqui");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh.jwt.aqui");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);
        when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);
        when(getUsersUseCase.findById(userId)).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("admin", "Admin123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("token.jwt.aqui"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("admin"));
    }

    @Test
    void login_con_credenciales_invalidas_retorna_401() throws Exception {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("admin", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void login_sin_body_retorna_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void login_username_en_blanco_retorna_400_con_mensaje() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("El usuario es obligatorio"));
    }
}

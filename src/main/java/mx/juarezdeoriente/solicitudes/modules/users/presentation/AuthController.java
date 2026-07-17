package mx.juarezdeoriente.solicitudes.modules.users.presentation;
import mx.juarezdeoriente.solicitudes.modules.users.application.dto.UserDto;
import mx.juarezdeoriente.solicitudes.modules.users.application.UserService;
import mx.juarezdeoriente.solicitudes.modules.users.domain.Role;
import mx.juarezdeoriente.solicitudes.modules.users.domain.User;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import mx.juarezdeoriente.solicitudes.security.AppUserDetails;
import mx.juarezdeoriente.solicitudes.security.AppUserDetailsService;
import mx.juarezdeoriente.solicitudes.security.JwtService;
import mx.juarezdeoriente.solicitudes.security.RefreshTokenService;
import mx.juarezdeoriente.solicitudes.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.web.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService            jwtService;
    private final RefreshTokenService   refreshTokenService;
    private final UserService           userService;
    private final AppUserDetailsService userDetailsService;

    @Value("${app.refresh-cookie.name:refresh_token}")
    private String cookieName;

    @Value("${app.refresh-cookie.same-site:Strict}")
    private String sameSite;

    @Value("${app.refresh-cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.refresh-cookie.path:/api/v1/auth}")
    private String cookiePath;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          UserService userService,
                          AppUserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService            = jwtService;
        this.refreshTokenService   = refreshTokenService;
        this.userService           = userService;
        this.userDetailsService    = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDto.LoginResponse>> login(
            @Valid @RequestBody UserDto.LoginRequest request,
            HttpServletResponse response) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        AppUserDetails principal = (AppUserDetails) auth.getPrincipal();
        String accessToken  = jwtService.generateToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        refreshTokenService.store(principal.getId(), refreshToken);
        setRefreshCookie(response, refreshToken);

        User user = userService.findById(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(new UserDto.LoginResponse(
                accessToken, "Bearer", jwtService.getExpirationMs() / 1000,
                new UserDto.LoginResponse.UserInfo(user.getId(), user.getUsername(),
                        user.getDisplayName(), user.getRoles()))));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<UserDto.LoginResponse>> refresh(
            @CookieValue(name = "${app.refresh-cookie.name:refresh_token}", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new DomainException("No hay sesion activa. Inicia sesion nuevamente.");
        }
        if (!jwtService.isValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            clearRefreshCookie(response);
            throw new DomainException("El refresh token es invalido o ha expirado. Inicia sesion nuevamente.");
        }

        UUID userId = refreshTokenService.validateAndRotate(refreshToken);
        AppUserDetails principal = (AppUserDetails)
                userDetailsService.loadUserByUsername(jwtService.extractUsername(refreshToken));

        String newAccessToken  = jwtService.generateToken(principal);
        String newRefreshToken = jwtService.generateRefreshToken(principal);
        refreshTokenService.store(userId, newRefreshToken);
        setRefreshCookie(response, newRefreshToken);

        User user = userService.findById(userId);
        return ResponseEntity.ok(ApiResponse.ok(new UserDto.LoginResponse(
                newAccessToken, "Bearer", jwtService.getExpirationMs() / 1000,
                new UserDto.LoginResponse.UserInfo(user.getId(), user.getUsername(),
                        user.getDisplayName(), user.getRoles()))));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AppUserDetails principal,
            HttpServletResponse response) {

        if (principal != null) refreshTokenService.revokeAll(principal.getId());
        clearRefreshCookie(response);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> me(
            @AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(UserDto.Response.from(userService.findById(principal.getId()))));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody UserDto.ChangePasswordRequest request,
            @AuthenticationPrincipal AppUserDetails principal,
            HttpServletResponse response) {

        userService.changePassword(principal.getId(), request.currentPassword(), request.newPassword());
        refreshTokenService.revokeAll(principal.getId());
        clearRefreshCookie(response);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from(cookieName, token)
                        .httpOnly(true).secure(secureCookie).sameSite(sameSite)
                        .path(cookiePath).maxAge(Duration.ofMillis(jwtService.getRefreshExpirationMs()))
                        .build().toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from(cookieName, "")
                        .httpOnly(true).secure(secureCookie).sameSite(sameSite)
                        .path(cookiePath).maxAge(0)
                        .build().toString());
    }
}

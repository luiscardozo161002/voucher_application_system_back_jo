package mx.juarezdeoriente.solicitudes.auth.infrastructure.web;

import jakarta.validation.Valid;
import mx.juarezdeoriente.solicitudes.auth.application.port.in.ChangePasswordUseCase;
import mx.juarezdeoriente.solicitudes.auth.application.port.in.GetUsersUseCase;
import mx.juarezdeoriente.solicitudes.auth.domain.model.User;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.AppUserDetails;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.AppUserDetailsService;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.JwtService;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.security.RefreshTokenService;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto.ChangePasswordRequest;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto.LoginRequest;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto.LoginResponse;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto.RefreshRequest;
import mx.juarezdeoriente.solicitudes.auth.infrastructure.web.dto.UserResponse;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.infrastructure.web.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager  authenticationManager;
    private final JwtService             jwtService;
    private final RefreshTokenService    refreshTokenService;
    private final ChangePasswordUseCase  changePasswordUseCase;
    private final GetUsersUseCase        getUsersUseCase;
    private final AppUserDetailsService  userDetailsService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          ChangePasswordUseCase changePasswordUseCase,
                          GetUsersUseCase getUsersUseCase,
                          AppUserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService            = jwtService;
        this.refreshTokenService   = refreshTokenService;
        this.changePasswordUseCase = changePasswordUseCase;
        this.getUsersUseCase       = getUsersUseCase;
        this.userDetailsService    = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        AppUserDetails principal = (AppUserDetails) auth.getPrincipal();
        String accessToken  = jwtService.generateToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        refreshTokenService.store(principal.getId(), refreshToken);

        User user = getUsersUseCase.findById(principal.getId());

        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(
                accessToken, refreshToken, "Bearer",
                jwtService.getExpirationMs() / 1000,
                jwtService.getRefreshExpirationMs() / 1000,
                new LoginResponse.UserInfo(user.getId(), user.getUsername(),
                        user.getDisplayName(), user.getRoles())
        )));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        String incomingRefresh = request.refreshToken();

        if (!jwtService.isValid(incomingRefresh) || !jwtService.isRefreshToken(incomingRefresh)) {
            throw new DomainException(
                    "El refresh token es invalido o ha expirado. Inicia sesion nuevamente.");
        }

        UUID userId = refreshTokenService.validateAndRotate(incomingRefresh);

        AppUserDetails principal = (AppUserDetails)
                userDetailsService.loadUserByUsername(jwtService.extractUsername(incomingRefresh));

        String newAccessToken  = jwtService.generateToken(principal);
        String newRefreshToken = jwtService.generateRefreshToken(principal);
        refreshTokenService.store(userId, newRefreshToken);

        User user = getUsersUseCase.findById(userId);

        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(
                newAccessToken, newRefreshToken, "Bearer",
                jwtService.getExpirationMs() / 1000,
                jwtService.getRefreshExpirationMs() / 1000,
                new LoginResponse.UserInfo(user.getId(), user.getUsername(),
                        user.getDisplayName(), user.getRoles())
        )));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AppUserDetails principal) {
        if (principal != null) {
            refreshTokenService.revokeAll(principal.getId());
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal AppUserDetails principal) {
        User user = getUsersUseCase.findById(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {

        changePasswordUseCase.execute(new ChangePasswordUseCase.Command(
                principal.getId(), request.currentPassword(), request.newPassword()
        ));
        refreshTokenService.revokeAll(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

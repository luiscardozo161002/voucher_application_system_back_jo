package mx.juarezdeoriente.solicitudes.model;

import mx.juarezdeoriente.solicitudes.security.RefreshTokenService;
import mx.juarezdeoriente.solicitudes.exception.ConflictException;
import mx.juarezdeoriente.solicitudes.exception.DomainException;
import mx.juarezdeoriente.solicitudes.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.shared.PageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository            userRepository;
    private final PasswordEncoder           passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final RefreshTokenService       refreshTokenService;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-minutes:15}")
    private int lockoutMinutes;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       ApplicationEventPublisher eventPublisher,
                       RefreshTokenService refreshTokenService) {
        this.userRepository      = userRepository;
        this.passwordEncoder     = passwordEncoder;
        this.eventPublisher      = eventPublisher;
        this.refreshTokenService = refreshTokenService;
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!user.isActive()) throw new DomainException("El usuario está desactivado");
        if (user.isLocked())  throw new DomainException("El usuario está bloqueado temporalmente. Intente más tarde.");

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.recordFailedLogin(maxLoginAttempts, lockoutMinutes);
            userRepository.save(user);
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        user.recordSuccessfulLogin();
        return userRepository.save(user);
    }

    public User create(String username, String password, String displayName,
                       String phone, Set<Role> roles) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("El nombre de usuario ya existe: " + username);
        }
        validatePasswordStrength(password);

        User user  = User.create(username, passwordEncoder.encode(password), displayName, phone, roles);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserEvents.Created(saved.getId(), saved.getUsername()));
        return saved;
    }

    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = findById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new DomainException("La contraseña actual es incorrecta");
        }
        validatePasswordStrength(newPassword);
        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario", id));
    }

    @Transactional(readOnly = true)
    public PageResult<User> findAll(int page, int size) {
        Page<User> p = userRepository.findAll(PageRequest.of(page, size));
        return PageResult.of(p.getContent(), page, size, p.getTotalElements());
    }

    public User update(UUID userId, String displayName, String phone,
                       Set<Role> roles, Boolean active, UUID requesterId) {
        if (userId.equals(requesterId) && Boolean.FALSE.equals(active)) {
            throw new DomainException("No puedes desactivar tu propio usuario");
        }

        User user = findById(userId);

        if (Boolean.FALSE.equals(active) && user.getRoles().contains(Role.ADMIN)
                && userRepository.countByRolesContainingAndActive("ADMIN", true) <= 1) {
            throw new DomainException("No se puede desactivar al único administrador activo del sistema");
        }

        boolean wasDeactivated = false;
        if (roles != null && !roles.isEmpty()) {
            user.update(displayName, phone, roles);
        }
        if (Boolean.TRUE.equals(active))  user.activate();
        if (Boolean.FALSE.equals(active)) {
            user.deactivate();
            wasDeactivated = true;
        }

        User saved = userRepository.save(user);
        if (wasDeactivated) {
            eventPublisher.publishEvent(new UserEvents.Deactivated(saved.getId(), saved.getUsername()));
        }
        eventPublisher.publishEvent(new UserEvents.Updated(userId, saved.getUsername(), requesterId));
        return saved;
    }

    public void delete(UUID targetId, UUID requesterId) {
        if (targetId.equals(requesterId)) {
            throw new DomainException("No puedes eliminar tu propio usuario");
        }
        User target = findById(targetId);
        if (target.getRoles().contains(Role.ADMIN)
                && userRepository.countByRolesContaining("ADMIN") <= 1) {
            throw new DomainException("No se puede eliminar al único administrador del sistema");
        }
        refreshTokenService.revokeAll(targetId);
        eventPublisher.publishEvent(new UserEvents.Deleted(targetId, target.getUsername(), requesterId));
        userRepository.deleteById(targetId);
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new DomainException("La contraseña debe tener al menos 8 caracteres");
        }
    }
}

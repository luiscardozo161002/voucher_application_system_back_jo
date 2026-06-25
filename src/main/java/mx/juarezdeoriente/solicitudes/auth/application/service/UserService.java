package mx.juarezdeoriente.solicitudes.auth.application.service;

import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;
import mx.juarezdeoriente.solicitudes.auth.domain.model.User;
import mx.juarezdeoriente.solicitudes.auth.domain.port.UserRepository;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.ConflictException;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository             userRepository;
    private final PasswordEncoder            passwordEncoder;
    private final ApplicationEventPublisher  eventPublisher;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-minutes:15}")
    private int lockoutMinutes;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher  = eventPublisher;
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

        User user = User.create(username, passwordEncoder.encode(password),
                displayName, phone, roles);
        User saved = userRepository.save(user);
        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
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
        return userRepository.findAll(page, size);
    }

    public User update(UUID userId, String displayName, String phone,
                       Set<Role> roles, Boolean active) {
        User user = findById(userId);

        if (roles != null && !roles.isEmpty()) {
            user.update(displayName, phone, roles);
        }
        if (Boolean.TRUE.equals(active))  user.activate();
        if (Boolean.FALSE.equals(active)) user.deactivate();

        User saved = userRepository.save(user);
        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
        return saved;
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new DomainException("La contraseña debe tener al menos 8 caracteres");
        }
    }
}

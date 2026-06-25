package mx.juarezdeoriente.solicitudes.auth.application.service;

import mx.juarezdeoriente.solicitudes.auth.application.port.in.*;
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

import java.util.UUID;

@Service
@Transactional
public class UserService implements LoginUseCase, CreateUserUseCase, ChangePasswordUseCase,
        GetUsersUseCase, UpdateUserUseCase {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

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

    // --- LoginUseCase ---

    @Override
    public User execute(LoginUseCase.Command command) {
        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!user.isActive()) {
            throw new DomainException("El usuario está desactivado");
        }
        if (user.isLocked()) {
            throw new DomainException("El usuario está bloqueado temporalmente. Intente más tarde.");
        }
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            user.recordFailedLogin(maxLoginAttempts, lockoutMinutes);
            userRepository.save(user);
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        user.recordSuccessfulLogin();
        return userRepository.save(user);
    }

    // --- CreateUserUseCase ---

    @Override
    public User execute(CreateUserUseCase.Command command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new ConflictException("El nombre de usuario ya existe: " + command.username());
        }
        validatePasswordStrength(command.password());

        User user = User.create(
                command.username(),
                passwordEncoder.encode(command.password()),
                command.displayName(),
                command.phone(),
                command.roles()
        );
        User saved = userRepository.save(user);
        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
        return saved;
    }

    // --- ChangePasswordUseCase ---

    @Override
    public void execute(ChangePasswordUseCase.Command command) {
        User user = findById(command.userId());

        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new DomainException("La contraseña actual es incorrecta");
        }
        validatePasswordStrength(command.newPassword());

        user.changePassword(passwordEncoder.encode(command.newPassword()));
        userRepository.save(user);
    }

    // --- GetUsersUseCase ---

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> findAll(int page, int size) {
        return userRepository.findAll(page, size);
    }

    // --- UpdateUserUseCase ---

    @Override
    public User execute(UpdateUserUseCase.Command command) {
        User user = findById(command.userId());

        if (command.roles() != null && !command.roles().isEmpty()) {
            user.update(command.displayName(), command.phone(), command.roles());
        }
        if (Boolean.TRUE.equals(command.active())) {
            user.activate();
        } else if (Boolean.FALSE.equals(command.active())) {
            user.deactivate();
        }

        User saved = userRepository.save(user);
        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
        return saved;
    }

    // --- Helpers ---

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new DomainException("La contraseña debe tener al menos 8 caracteres");
        }
    }
}

package mx.juarezdeoriente.solicitudes.auth.infrastructure.persistence;

import mx.juarezdeoriente.solicitudes.auth.domain.model.User;
import mx.juarezdeoriente.solicitudes.auth.domain.port.UserRepository;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida: traduce entre el dominio (User) y la persistencia (UserJpaEntity).
 * El dominio nunca conoce JPA.
 */
@Component
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity e = jpaRepository.findById(user.getId()).orElse(new UserJpaEntity());
        e.setId(user.getId());
        e.setUsername(user.getUsername());
        e.setPasswordHash(user.getPasswordHash());
        e.setDisplayName(user.getDisplayName());
        e.setPhone(user.getPhone());
        e.setRoles(user.getRoles());
        e.setActive(user.isActive());
        e.setRequiresPasswordReset(user.isRequiresPasswordReset());
        e.setFailedLoginAttempts(user.getFailedLoginAttempts());
        e.setLockedUntil(user.getLockedUntil());
        e.setTokenVersion(user.getTokenVersion());
        e.setCreatedAt(user.getCreatedAt());
        e.setUpdatedAt(java.time.Instant.now());
        return toDomain(jpaRepository.save(e));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public PageResult<User> findAll(int page, int size) {
        Page<UserJpaEntity> p = jpaRepository.findAll(PageRequest.of(page, size));
        return PageResult.of(p.getContent().stream().map(this::toDomain).toList(),
                page, size, p.getTotalElements());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public long countByRole(String role) {
        return jpaRepository.countByRolesContaining(role);
    }

    @Override
    public long countByRoleAndActive(String role, boolean active) {
        return jpaRepository.countByRolesContainingAndActive(role, active);
    }

    private UserJpaEntity toEntity(User u) {
        UserJpaEntity e = new UserJpaEntity();
        e.setId(u.getId());
        e.setUsername(u.getUsername());
        e.setPasswordHash(u.getPasswordHash());
        e.setDisplayName(u.getDisplayName());
        e.setPhone(u.getPhone());
        e.setRoles(u.getRoles());
        e.setActive(u.isActive());
        e.setRequiresPasswordReset(u.isRequiresPasswordReset());
        e.setFailedLoginAttempts(u.getFailedLoginAttempts());
        e.setLockedUntil(u.getLockedUntil());
        e.setCreatedAt(u.getCreatedAt());
        return e;
    }

    private User toDomain(UserJpaEntity e) {
        return User.reconstitute(
                e.getId(), e.getUsername(), e.getPasswordHash(),
                e.getDisplayName(), e.getPhone(), e.getRoles(),
                e.isActive(), e.isRequiresPasswordReset(),
                e.getFailedLoginAttempts(), e.getLockedUntil(),
                e.getCreatedAt(), e.getTokenVersion()
        );
    }
}

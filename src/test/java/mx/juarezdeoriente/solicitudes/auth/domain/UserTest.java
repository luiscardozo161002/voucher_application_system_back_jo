package mx.juarezdeoriente.solicitudes.auth.domain;

import mx.juarezdeoriente.solicitudes.auth.domain.event.UserCreatedEvent;
import mx.juarezdeoriente.solicitudes.auth.domain.event.UserDeactivatedEvent;
import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;
import mx.juarezdeoriente.solicitudes.auth.domain.model.User;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.domain.model.DomainEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    void crear_usuario_registra_evento_UserCreated() {
        User user = User.create("jperez", "hash", "Juan Pérez", "555-0001", Set.of(Role.CAPTURISTA));

        List<DomainEvent> events = user.pullDomainEvents();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(UserCreatedEvent.class);
        assertThat(((UserCreatedEvent) events.get(0)).getUsername()).isEqualTo("jperez");
    }

    @Test
    void crear_usuario_sin_roles_lanza_DomainException() {
        assertThatThrownBy(() -> User.create("jperez", "hash", "Juan Pérez", null, Set.of()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("rol");
    }

    @Test
    void desactivar_usuario_registra_evento_UserDeactivated() {
        User user = User.create("jperez", "hash", "Juan Pérez", null, Set.of(Role.CAPTURISTA));
        user.pullDomainEvents(); // limpia el evento de creación

        user.deactivate();

        List<DomainEvent> events = user.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(UserDeactivatedEvent.class);
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void pullDomainEvents_limpia_la_lista() {
        User user = User.create("jperez", "hash", "Juan Pérez", null, Set.of(Role.ADMIN));

        user.pullDomainEvents();

        assertThat(user.pullDomainEvents()).isEmpty();
    }

    @Test
    void bloqueo_temporal_se_activa_al_superar_intentos_fallidos() {
        User user = User.create("jperez", "hash", "Juan Pérez", null, Set.of(Role.CAPTURISTA));
        user.pullDomainEvents();

        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(5, 15);
        }

        assertThat(user.isLocked()).isTrue();
    }

    @Test
    void bloqueo_se_libera_tras_login_exitoso() {
        User user = User.create("jperez", "hash", "Juan Pérez", null, Set.of(Role.CAPTURISTA));
        user.pullDomainEvents();

        for (int i = 0; i < 5; i++) user.recordFailedLogin(5, 15);
        assertThat(user.isLocked()).isTrue();

        user.recordSuccessfulLogin();

        assertThat(user.isLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }
}

package mx.juarezdeoriente.solicitudes.auth.domain;

import mx.juarezdeoriente.solicitudes.auth.Role;
import mx.juarezdeoriente.solicitudes.auth.User;
import mx.juarezdeoriente.solicitudes.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    void crear_usuario_sin_roles_lanza_DomainException() {
        assertThatThrownBy(() -> User.create("jperez", "hash", "Juan Pérez", null, Set.of()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("rol");
    }

    @Test
    void desactivar_usuario_cambia_active_a_false() {
        User user = User.create("jperez", "hash", "Juan Pérez", null, Set.of(Role.CAPTURISTA));

        user.deactivate();

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void bloqueo_temporal_se_activa_al_superar_intentos_fallidos() {
        User user = User.create("jperez", "hash", "Juan Pérez", null, Set.of(Role.CAPTURISTA));

        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(5, 15);
        }

        assertThat(user.isLocked()).isTrue();
    }

    @Test
    void bloqueo_se_libera_tras_login_exitoso() {
        User user = User.create("jperez", "hash", "Juan Pérez", null, Set.of(Role.CAPTURISTA));

        for (int i = 0; i < 5; i++) user.recordFailedLogin(5, 15);
        assertThat(user.isLocked()).isTrue();

        user.recordSuccessfulLogin();

        assertThat(user.isLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }
}

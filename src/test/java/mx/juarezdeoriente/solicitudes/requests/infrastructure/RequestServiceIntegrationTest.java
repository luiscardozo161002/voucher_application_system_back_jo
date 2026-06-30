package mx.juarezdeoriente.solicitudes.requests.infrastructure;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import mx.juarezdeoriente.solicitudes.IntegrationTestBase;
import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;
import mx.juarezdeoriente.solicitudes.auth.domain.model.User;
import mx.juarezdeoriente.solicitudes.auth.domain.port.UserRepository;
import mx.juarezdeoriente.solicitudes.requests.application.service.RequestService;
import mx.juarezdeoriente.solicitudes.requests.domain.model.Request;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestStatus;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import mx.juarezdeoriente.solicitudes.suppliers.application.service.SupplierService;
import mx.juarezdeoriente.solicitudes.suppliers.domain.model.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@Transactional
@EnabledIfSystemProperty(named = "integration.tests", matches = "true")
class RequestServiceIntegrationTest extends IntegrationTestBase {

    @Autowired RequestService  requestService;
    @Autowired SupplierService supplierService;
    @Autowired UserRepository  userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Supplier supplier;
    private User     user;

    @BeforeEach
    void setUp() {
        supplier = supplierService.create("PROV-001", "Proveedor de Prueba", "555-9999");
        user = userRepository.save(
                User.create("test_user", passwordEncoder.encode("Password1!"),
                        "Usuario de Prueba", null, Set.of(Role.CAPTURISTA))
        );
        user.pullDomainEvents(); // descarta eventos de creación
    }

    @Test
    void crear_borrador_y_emitir_asigna_folio_unico() {
        Request r1 = requestService.createDraft(supplier.getId(), null, "Materiales de oficina", "Gerente", user.getId());
        requestService.addItem(r1.getId(), null, "Papel A4",
                new BigDecimal("5"), "Resma", new BigDecimal("120.00"));

        Request r2 = requestService.createDraft(supplier.getId(), null, "Limpieza", "Gerente", user.getId());
        requestService.addItem(r2.getId(), null, "Escoba",
                new BigDecimal("2"), "PZA", new BigDecimal("45.00"));

        Request emitida1 = requestService.issue(r1.getId());
        Request emitida2 = requestService.issue(r2.getId());

        assertThat(emitida1.getFolio()).isNotNull();
        assertThat(emitida2.getFolio()).isNotNull();
        assertThat(emitida1.getFolio()).isNotEqualTo(emitida2.getFolio());
        assertThat(emitida1.getStatus()).isEqualTo(RequestStatus.EMITIDA);
    }

    @Test
    void cancelar_solicitud_emitida_cambia_estado_y_persiste_motivo() {
        Request draft = requestService.createDraft(supplier.getId(), null, "Compra de equipo", "Director", user.getId());
        requestService.addItem(draft.getId(), null, "Computadora",
                BigDecimal.ONE, "PZA", new BigDecimal("18000.00"));

        Request emitida  = requestService.issue(draft.getId());
        Request cancelada = requestService.cancel(emitida.getId(), "Presupuesto cancelado", user.getId());

        assertThat(cancelada.getStatus()).isEqualTo(RequestStatus.CANCELADA);
        assertThat(cancelada.getCancellationReason()).isEqualTo("Presupuesto cancelado");
        assertThat(cancelada.getFolio()).isEqualTo(emitida.getFolio()); // folio se conserva
    }

    @Test
    void emitir_borrador_sin_renglones_lanza_DomainException() {
        Request draft = requestService.createDraft(supplier.getId(), null, "Sin renglones", null, user.getId());

        assertThatThrownBy(() -> requestService.issue(draft.getId()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("renglón");
    }

    @Test
    void busqueda_por_estado_retorna_solo_los_correctos() {
        Request d1 = requestService.createDraft(supplier.getId(), null, "Destino A", null, user.getId());
        requestService.addItem(d1.getId(), null, "Art. 1", BigDecimal.ONE, "PZA", BigDecimal.TEN);
        requestService.issue(d1.getId());

        Request d2 = requestService.createDraft(supplier.getId(), null, "Destino B", null, user.getId());

        var emitidas = requestService.search(null, null, null, null, null,
                RequestStatus.EMITIDA, null, 0, 20);

        assertThat(emitidas.content()).allMatch(r -> r.getStatus() == RequestStatus.EMITIDA);
        assertThat(emitidas.content().stream().map(Request::getId))
                .doesNotContain(d2.getId());
    }
}

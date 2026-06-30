package mx.juarezdeoriente.solicitudes.requests.domain;

import mx.juarezdeoriente.solicitudes.requests.domain.event.RequestCancelledEvent;
import mx.juarezdeoriente.solicitudes.requests.domain.event.RequestIssuedEvent;
import mx.juarezdeoriente.solicitudes.requests.domain.model.Request;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestItem;
import mx.juarezdeoriente.solicitudes.requests.domain.model.RequestStatus;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class RequestTest {

    private static final UUID SUPPLIER_ID  = UUID.randomUUID();
    private static final UUID CREATED_BY   = UUID.randomUUID();

    private Request draft;

    @BeforeEach
    void setUp() {
        draft = Request.createDraft(SUPPLIER_ID, null, "Mantenimiento de oficinas", "Gerente", CREATED_BY);
    }

    @Test
    void crear_borrador_inicia_en_estado_BORRADOR() {
        assertThat(draft.getStatus()).isEqualTo(RequestStatus.BORRADOR);
        assertThat(draft.getFolio()).isNull();
    }

    @Test
    void crear_borrador_sin_proveedor_lanza_DomainException() {
        assertThatThrownBy(() -> Request.createDraft(null, null, "Destino", "Autor", CREATED_BY))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void crear_borrador_sin_destino_lanza_DomainException() {
        assertThatThrownBy(() -> Request.createDraft(SUPPLIER_ID, null, "  ", "Autor", CREATED_BY))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void emitir_solicitud_asigna_folio_y_cambia_estado() {
        draft.addItem(itemConDescripcion("Material de limpieza"));

        draft.issue(704L);

        assertThat(draft.getStatus()).isEqualTo(RequestStatus.EMITIDA);
        assertThat(draft.getFolio()).isEqualTo(704L);
        assertThat(draft.getIssuedAt()).isNotNull();
    }

    @Test
    void emitir_registra_evento_RequestIssued() {
        draft.addItem(itemConDescripcion("Material de limpieza"));
        draft.issue(704L);

        var events = draft.pullDomainEvents();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(RequestIssuedEvent.class);
        assertThat(((RequestIssuedEvent) events.get(0)).getFolio()).isEqualTo(704L);
    }

    @Test
    void emitir_sin_renglones_lanza_DomainException() {
        assertThatThrownBy(() -> draft.issue(704L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("rengl");
    }

    @Test
    void cancelar_solicitud_emitida_registra_evento_y_cambia_estado() {
        draft.addItem(itemConDescripcion("Servicio de limpieza"));
        draft.issue(705L);
        draft.pullDomainEvents();

        draft.cancel("Proveedor no disponible", CREATED_BY);

        assertThat(draft.getStatus()).isEqualTo(RequestStatus.CANCELADA);
        assertThat(draft.getCancellationReason()).isEqualTo("Proveedor no disponible");

        var events = draft.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(RequestCancelledEvent.class);
    }

    @Test
    void cancelar_sin_motivo_lanza_DomainException() {
        draft.addItem(itemConDescripcion("ArtÃ­culo"));
        draft.issue(706L);

        assertThatThrownBy(() -> draft.cancel("  ", CREATED_BY))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void cancelar_una_solicitud_ya_cancelada_lanza_DomainException() {
        draft.addItem(itemConDescripcion("ArtÃ­culo"));
        draft.issue(707L);
        draft.cancel("Motivo inicial", CREATED_BY);
        draft.pullDomainEvents();

        assertThatThrownBy(() -> draft.cancel("Otro motivo", CREATED_BY))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void no_se_puede_agregar_mas_de_8_renglones() {
        for (int i = 1; i <= 8; i++) {
            draft.addItem(itemConDescripcion("ArtÃ­culo " + i));
        }

        assertThatThrownBy(() -> draft.addItem(itemConDescripcion("ArtÃ­culo 9")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("8");
    }

    @Test
    void se_puede_agregar_un_renglon_a_una_solicitud_emitida() {
        draft.addItem(itemConDescripcion("Articulo"));
        draft.issue(708L);
        draft.pullDomainEvents();

        draft.addItem(itemConDescripcion("Otro articulo"));

        assertThat(draft.getItems()).hasSize(2);
    }

    @Test
    void restaurar_solicitud_emitida_eliminada_vuelve_a_EMITIDA() {
        draft.addItem(itemConDescripcion("Articulo"));
        draft.issue(709L);
        draft.cancel("Solicitud eliminada", CREATED_BY);

        draft.restore();

        assertThat(draft.getStatus()).isEqualTo(RequestStatus.EMITIDA);
        assertThat(draft.getFolio()).isEqualTo(709L);
        assertThat(draft.getIssuedAt()).isNotNull();
        assertThat(draft.getCancelledAt()).isNull();
        assertThat(draft.getCancellationReason()).isNull();
    }

    @Test
    void eliminar_un_renglon_reduce_la_lista() {
        RequestItem item = itemConDescripcion("ArtÃ­culo a eliminar");
        draft.addItem(item);
        assertThat(draft.getItems()).hasSize(1);

        draft.removeItem(item.getId());

        assertThat(draft.getItems()).isEmpty();
    }

    // ---- helpers ----

    private RequestItem itemConDescripcion(String descripcion) {
        return RequestItem.create(
                UUID.randomUUID(), descripcion,
                new BigDecimal("2"), "PZA",
                new BigDecimal("150.00"),
                draft.getItems().size() + 1
        );
    }
}

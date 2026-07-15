package mx.juarezdeoriente.solicitudes.requests.domain;

import mx.juarezdeoriente.solicitudes.requests.RequestItem;
import mx.juarezdeoriente.solicitudes.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class RequestItemTest {

    @Test
    void total_se_calcula_como_cantidad_por_costo_unitario() {
        RequestItem item = RequestItem.create(
                UUID.randomUUID(), "Silla de oficina",
                new BigDecimal("3"), "PZA",
                new BigDecimal("850.50"), 1
        );

        assertThat(item.getTotal()).isEqualByComparingTo(new BigDecimal("2551.50"));
    }

    @Test
    void total_es_cero_cuando_cantidad_es_null() {
        RequestItem item = RequestItem.create(UUID.randomUUID(), "Servicio", null, null, null, 1);
        assertThat(item.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void crear_item_sin_descripcion_lanza_DomainException() {
        assertThatThrownBy(() ->
                RequestItem.create(UUID.randomUUID(), "  ", BigDecimal.ONE, "PZA", BigDecimal.TEN, 1)
        ).isInstanceOf(DomainException.class).hasMessageContaining("descripción");
    }
}

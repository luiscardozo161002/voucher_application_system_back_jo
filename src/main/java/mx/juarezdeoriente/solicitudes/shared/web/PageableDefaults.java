package mx.juarezdeoriente.solicitudes.shared.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Aplica límites de paginación para evitar consultas que devuelvan
 * demasiados registros y degraden el rendimiento.
 */
@Component
public class PageableDefaults {

    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    public int safeSize(int requested) {
        if (requested < 1)  return 10;
        if (requested > maxPageSize) return maxPageSize;
        return requested;
    }

    public int safePage(int requested) {
        return Math.max(0, requested);
    }
}

package mx.juarezdeoriente.solicitudes.shared.model;

import java.util.List;
import java.util.function.Function;

/**
 * Resultado paginado genérico. Independiente de Spring Data para que
 * el dominio y la aplicación no dependan de infraestructura.
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResult<>(content, page, size, totalElements, totalPages);
    }

    /** Transforma el contenido sin perder la metadata de paginación. */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(
                content.stream().map(mapper).toList(),
                page, size, totalElements, totalPages
        );
    }
}

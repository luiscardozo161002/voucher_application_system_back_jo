package mx.juarezdeoriente.solicitudes.shared.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envoltorio uniforme para todas las respuestas REST.
 * En éxito: { "data": ... }
 * En error: { "error": "...", "details": [...] }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, String error, Object details) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(null, message, null);
    }

    public static ApiResponse<Void> error(String message, Object details) {
        return new ApiResponse<>(null, message, details);
    }
}

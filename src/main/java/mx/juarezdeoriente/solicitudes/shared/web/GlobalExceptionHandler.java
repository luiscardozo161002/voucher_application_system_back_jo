package mx.juarezdeoriente.solicitudes.shared.web;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import mx.juarezdeoriente.solicitudes.shared.exception.ConflictException;
import mx.juarezdeoriente.solicitudes.shared.exception.DomainException;
import mx.juarezdeoriente.solicitudes.shared.exception.NotFoundException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Errores de dominio ---

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomain(DomainException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // --- Errores de validacion de entrada ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Hay campos requeridos o con formato incorrecto.", detalles));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedBody(HttpMessageNotReadableException ex) {
        String causa = ex.getMessage() != null ? ex.getMessage() : "";
        String mensaje;
        if (causa.contains("not one of the values accepted for Enum")) {
            mensaje = "Uno de los campos tiene un valor no permitido. Verifica los valores aceptados (ej. SOCIO/EVENTUAL para tipo de trabajador, ADMIN/CAPTURISTA para roles).";
        } else {
            mensaje = "El cuerpo de la peticion no es un JSON valido o esta vacio. Verifica la sintaxis.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(mensaje));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String campo = ex.getName();
        String valor = ex.getValue() != null ? ex.getValue().toString() : "null";
        String tipo  = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "El parametro '" + campo + "' tiene un valor invalido: '" + valor
                        + "'. Se esperaba: " + tipo + "."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("El parametro '" + ex.getParameterName() + "' es obligatorio."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("El metodo HTTP '" + ex.getMethod() + "' no esta permitido en esta ruta."));
    }

    // --- Errores de autenticacion y autorizacion ---

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Usuario o contrasena incorrectos."));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Tu cuenta esta desactivada. Contacta al administrador."));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Cuenta bloqueada temporalmente por multiples intentos fallidos. Intenta de nuevo en unos minutos."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permiso para realizar esta accion. Verifica tu rol."));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwt(ExpiredJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Tu sesion ha expirado. Inicia sesion nuevamente."));
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJwt(MalformedJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("El token de autenticacion es invalido. Inicia sesion nuevamente."));
    }

    // --- Concurrencia ---

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "El registro fue modificado por otro usuario. Recarga la informacion e intenta de nuevo."));
    }

    // --- Error generico ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Error inesperado no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Ocurrio un error interno. Intenta de nuevo o contacta al administrador."));
    }
}

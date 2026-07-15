package mx.juarezdeoriente.solicitudes.exception;

/** Excepción de regla de negocio. Siempre lleva un mensaje para el cliente. */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}

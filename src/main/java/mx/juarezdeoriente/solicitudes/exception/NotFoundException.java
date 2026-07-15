package mx.juarezdeoriente.solicitudes.exception;

public class NotFoundException extends DomainException {

    public NotFoundException(String entity, Object id) {
        super(entity + " no encontrado: " + id);
    }
}

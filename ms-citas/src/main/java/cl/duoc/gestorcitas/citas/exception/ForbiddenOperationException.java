package cl.duoc.gestorcitas.citas.exception;

/** El usuario autenticado no tiene permiso sobre el recurso (HTTP 403). */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}

package cl.duoc.gestorcitas.usuarios.exception;

/** Violación de una regla de negocio (se responde con HTTP 409). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

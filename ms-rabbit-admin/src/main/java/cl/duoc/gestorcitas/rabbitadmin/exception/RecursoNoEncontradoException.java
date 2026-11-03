package cl.duoc.gestorcitas.rabbitadmin.exception;

/** 404 Not Found: la cola, exchange o binding no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }

    public RecursoNoEncontradoException(String message, Throwable cause) {
        super(message, cause);
    }
}

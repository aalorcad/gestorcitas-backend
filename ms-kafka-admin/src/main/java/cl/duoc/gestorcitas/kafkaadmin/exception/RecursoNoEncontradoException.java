package cl.duoc.gestorcitas.kafkaadmin.exception;

/** 404 Not Found: el tópico o el grupo de consumidores no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }

    public RecursoNoEncontradoException(String message, Throwable cause) {
        super(message, cause);
    }
}

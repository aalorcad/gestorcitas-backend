package cl.duoc.gestorcitas.kafkaadmin.exception;

/** 409 Conflict: el tópico ya existe, está protegido o el grupo tiene consumidores activos. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String message) {
        super(message);
    }

    public ConflictoException(String message, Throwable cause) {
        super(message, cause);
    }
}

package cl.duoc.gestorcitas.rabbitadmin.exception;

/** 409 Conflict: el recurso ya existe, está protegido o RabbitMQ rechazó la operación. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String message) {
        super(message);
    }

    public ConflictoException(String message, Throwable cause) {
        super(message, cause);
    }
}

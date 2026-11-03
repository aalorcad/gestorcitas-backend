package cl.duoc.gestorcitas.notificaciones.exception;

/**
 * Error NO recuperable: el mensaje está mal formado o incompleto.
 * Reintentarlo no sirve de nada, por lo que se rechaza (NACK) y va directo a la DLQ.
 */
public class MensajeInvalidoException extends RuntimeException {

    public MensajeInvalidoException(String message) {
        super(message);
    }

    public MensajeInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}

package cl.duoc.gestorcitas.rabbitadmin.exception;

/** 400 Bad Request: la petición es válida en formato pero no tiene sentido para RabbitMQ. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String message) {
        super(message);
    }

    public SolicitudInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}

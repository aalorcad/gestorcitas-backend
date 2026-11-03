package cl.duoc.gestorcitas.rabbitadmin.exception;

/** 503 Service Unavailable: RabbitMQ o su API de management no responden. */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String message) {
        super(message);
    }

    public ServicioNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}

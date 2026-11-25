package cl.duoc.gestorcitas.kafkaadmin.exception;

/** 503 Service Unavailable: el clúster Kafka no responde. */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String message) {
        super(message);
    }

    public ServicioNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}

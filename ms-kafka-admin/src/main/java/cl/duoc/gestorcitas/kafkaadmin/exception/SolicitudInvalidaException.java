package cl.duoc.gestorcitas.kafkaadmin.exception;

/** 400 Bad Request: la petición es válida en formato pero Kafka la rechaza (particiones, replicación o configuración inválidas). */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String message) {
        super(message);
    }

    public SolicitudInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}

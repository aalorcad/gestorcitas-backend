package cl.duoc.gestorcitas.citas.exception;

/** Falla de comunicación con otro microservicio (HTTP 502). */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

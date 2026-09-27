package cl.duoc.gestorcitas.usuarios.exception;

/** Falla de comunicación con otro microservicio (HTTP 502). */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

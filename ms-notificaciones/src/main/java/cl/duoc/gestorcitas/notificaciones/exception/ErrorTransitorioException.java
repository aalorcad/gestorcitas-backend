package cl.duoc.gestorcitas.notificaciones.exception;

/**
 * Error RECUPERABLE: una dependencia externa falló de forma temporal (servidor de correo, disco, etc.).
 * El mensaje se reintenta con espera; si se agotan los intentos va a la DLQ.
 */
public class ErrorTransitorioException extends RuntimeException {

    public ErrorTransitorioException(String message) {
        super(message);
    }

    public ErrorTransitorioException(String message, Throwable cause) {
        super(message, cause);
    }
}

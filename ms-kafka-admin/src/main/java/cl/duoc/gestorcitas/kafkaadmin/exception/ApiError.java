package cl.duoc.gestorcitas.kafkaadmin.exception;

import java.time.OffsetDateTime;
import java.util.List;

/** Formato de error común a todos los microservicios de GestorCitas. */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
    public static ApiError of(int status, String error, String message, String path, List<String> details) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, details);
    }
}

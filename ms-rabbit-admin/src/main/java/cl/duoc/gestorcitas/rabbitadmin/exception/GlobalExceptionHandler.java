package cl.duoc.gestorcitas.rabbitadmin.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/** Traduce las excepciones a respuestas JSON con mensajes claros (formato {@link ApiError}). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DATOS_INVALIDOS = "Datos de entrada inválidos";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiError> conflicto(ConflictoException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ApiError> solicitudInvalida(SolicitudInvalidaException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ApiError> noDisponible(ServicioNoDisponibleException ex, HttpServletRequest req) {
        log.warn("RabbitMQ no disponible: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req, List.of());
    }

    /** @Valid sobre @RequestBody (MethodArgumentNotValidException) y @ModelAttribute (BindException). */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> validacion(BindException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e instanceof FieldError fe ? fe.getField() + ": " + fe.getDefaultMessage() : e.getDefaultMessage())
                .sorted()
                .toList();
        return build(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, req, detalles);
    }

    /** Restricciones en @PathVariable / @RequestParam (validación de métodos de Spring MVC 6.1+). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @SuppressWarnings("deprecation") // getAllValidationResults existe desde Spring 6.1 (compatible con 6.1 y 6.2)
    public ResponseEntity<ApiError> validacionMetodo(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<String> detalles = ex.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> nombreParametro(r.getMethodParameter().getParameterName()) + ": " + mensaje(e)))
                .sorted()
                .toList();
        return build(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, req, detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> restricciones(ConstraintViolationException ex, HttpServletRequest req) {
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
        return build(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, req, detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        Throwable causa = ex.getMostSpecificCause();
        String detalle = causa instanceof IllegalArgumentException ? causa.getMessage() : "El cuerpo no es un JSON válido";
        return build(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, req, List.of(detalle));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> parametro(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generico(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, List.of());
    }

    private static String nombreParametro(String nombre) {
        return nombre == null ? "parámetro" : nombre;
    }

    private static String mensaje(MessageSourceResolvable error) {
        return error.getDefaultMessage() == null
                ? new DefaultMessageSourceResolvable(error.getCodes(), "inválido").getDefaultMessage()
                : error.getDefaultMessage();
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String msg, HttpServletRequest req, List<String> details) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), msg, req.getRequestURI(), details));
    }
}

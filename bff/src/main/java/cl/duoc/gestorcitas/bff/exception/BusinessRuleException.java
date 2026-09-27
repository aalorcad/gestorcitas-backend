package cl.duoc.gestorcitas.bff.exception;

/** Regla de negocio que cruza microservicios y se valida en la orquestación del BFF (HTTP 409). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}

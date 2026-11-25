package cl.duoc.gestorcitas.analitica.messaging.support;

import cl.duoc.gestorcitas.analitica.exception.EventoInvalidoException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/** Aplica Bean Validation a los eventos recibidos; si no cumplen el contrato lanza {@link EventoInvalidoException}. */
@Component
public class ValidadorDeEventos {

    private final Validator validator;

    public ValidadorDeEventos(Validator validator) {
        this.validator = validator;
    }

    public <T> void validar(T evento, String contexto) {
        if (evento == null) {
            throw new EventoInvalidoException("Registro sin contenido " + contexto);
        }
        Set<ConstraintViolation<T>> errores = validator.validate(evento);
        if (!errores.isEmpty()) {
            String detalle = errores.stream()
                    .map(e -> e.getPropertyPath() + " " + e.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new EventoInvalidoException("Evento inválido [" + detalle + "] " + contexto);
        }
    }
}

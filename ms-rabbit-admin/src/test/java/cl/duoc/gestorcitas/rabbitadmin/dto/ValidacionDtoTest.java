package cl.duoc.gestorcitas.rabbitadmin.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** La API no debe aceptar colas con nombres vacíos ni configuraciones inválidas. */
class ValidacionDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void colaValidaNoTieneErrores() {
        assertThat(errores(new CrearColaRequest("q.demo", true, false, 60_000, 100, "gestorcitas.dlx", "q.demo.dlq")))
                .isEmpty();
    }

    @Test
    void nombreVacioEsInvalido() {
        assertThat(errores(cola(""))).contains("nombre");
        assertThat(errores(cola(null))).contains("nombre");
        assertThat(errores(cola("   "))).contains("nombre");
    }

    @Test
    void nombreReservadoOConCaracteresInvalidosEsInvalido() {
        assertThat(errores(cola("amq.mia"))).contains("nombre");
        assertThat(errores(cola("cola con espacios"))).contains("nombre");
        assertThat(errores(cola("cola/raro"))).contains("nombre");
        assertThat(errores(cola("x".repeat(256)))).contains("nombre");
    }

    @Test
    void ttlYLargoMaximoDebenSerPositivos() {
        assertThat(errores(new CrearColaRequest("q.demo", true, false, 0, -5, null, null)))
                .contains("ttlMs", "maxMensajes");
    }

    @Test
    void routingKeyDeDeadLetterRequiereExchange() {
        assertThat(errores(new CrearColaRequest("q.demo", true, false, null, null, null, "q.demo.dlq")))
                .contains("deadLetterCoherente");
    }

    @Test
    void autoDeleteNoPuedeSerDurable() {
        assertThat(errores(new CrearColaRequest("q.demo", true, true, null, null, null, null)))
                .contains("autoDeleteCoherente");
        assertThat(errores(new CrearColaRequest("q.demo", false, true, null, null, null, null))).isEmpty();
    }

    @Test
    void exchangeRequiereTipo() {
        assertThat(errores(new CrearExchangeRequest("ex.demo", null, true, false))).contains("tipo");
    }

    @Test
    void bindingRequiereRoutingKeyValida() {
        assertThat(errores(new BindingRequest("ex.demo", "q.demo", "cita.*"))).isEmpty();
        assertThat(errores(new BindingRequest("ex.demo", "q.demo", null))).contains("routingKey");
        assertThat(errores(new BindingRequest("ex.demo", "q.demo", "con espacio"))).contains("routingKey");
        assertThat(errores(new BindingRequest("", "q.demo", "x"))).contains("exchange");
    }

    private static CrearColaRequest cola(String nombre) {
        return new CrearColaRequest(nombre, null, null, null, null, null, null);
    }

    private <T> Set<String> errores(T dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}

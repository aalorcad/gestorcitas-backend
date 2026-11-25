package cl.duoc.gestorcitas.kafkaadmin;

import cl.duoc.gestorcitas.kafkaadmin.dto.AumentarParticionesRequest;
import cl.duoc.gestorcitas.kafkaadmin.dto.CrearTopicoRequest;
import cl.duoc.gestorcitas.kafkaadmin.dto.EstrategiaReinicio;
import cl.duoc.gestorcitas.kafkaadmin.dto.ReiniciarOffsetsRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Reglas de entrada de la API: nombres Kafka válidos, rangos de particiones/replicación y coherencia del reinicio. */
class ValidacionDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void topicoValidoNoTieneErrores() {
        assertThat(errores(new CrearTopicoRequest("demo.topico_1-a", 3, 3, Map.of("retention.ms", "60000")))).isEmpty();
    }

    @Test
    void nombreDeTopicoInvalido() {
        assertThat(errores(topico(""))).contains("nombre");
        assertThat(errores(topico(null))).contains("nombre");
        assertThat(errores(topico("con espacio"))).contains("nombre");
        assertThat(errores(topico("tópico"))).contains("nombre");
        assertThat(errores(topico("x".repeat(250)))).contains("nombre");
    }

    @Test
    void particionesYReplicacionFueraDeRango() {
        assertThat(errores(new CrearTopicoRequest("t", 0, 1, null))).containsExactly("particiones");
        assertThat(errores(new CrearTopicoRequest("t", 51, 1, null))).containsExactly("particiones");
        assertThat(errores(new CrearTopicoRequest("t", 1, 4, null))).containsExactly("factorReplicacion");
        assertThat(errores(new CrearTopicoRequest("t", null, null, null))).contains("particiones", "factorReplicacion");
        assertThat(errores(new AumentarParticionesRequest(0))).containsExactly("total");
    }

    @Test
    void reinicioConOffsetExactoExigeOffset() {
        assertThat(errores(new ReiniciarOffsetsRequest("t", EstrategiaReinicio.OFFSET, null))).contains("offsetCoherente");
        assertThat(errores(new ReiniciarOffsetsRequest("t", EstrategiaReinicio.OFFSET, 5L))).isEmpty();
        assertThat(errores(new ReiniciarOffsetsRequest("t", EstrategiaReinicio.EARLIEST, null))).isEmpty();
        assertThat(errores(new ReiniciarOffsetsRequest("t", null, null))).contains("estrategia");
        assertThat(errores(new ReiniciarOffsetsRequest("t", EstrategiaReinicio.OFFSET, -1L))).contains("offset");
    }

    private static CrearTopicoRequest topico(String nombre) {
        return new CrearTopicoRequest(nombre, 1, 1, null);
    }

    private <T> Set<String> errores(T objeto) {
        Set<ConstraintViolation<T>> v = validator.validate(objeto);
        return v.stream().map(c -> c.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}

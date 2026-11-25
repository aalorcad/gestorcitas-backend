package cl.duoc.gestorcitas.kafkaadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Reinicio de offsets de un grupo en un tópico. El grupo debe estar SIN consumidores activos.
 *
 * @param offset obligatorio solo con la estrategia OFFSET
 */
public record ReiniciarOffsetsRequest(
        @NombreTopico String topico,
        @NotNull(message = "es obligatoria (EARLIEST, LATEST u OFFSET)") EstrategiaReinicio estrategia,
        @PositiveOrZero(message = "no puede ser negativo") Long offset
) {
    @AssertTrue(message = "offset es obligatorio con la estrategia OFFSET")
    public boolean isOffsetCoherente() {
        return estrategia != EstrategiaReinicio.OFFSET || offset != null;
    }
}

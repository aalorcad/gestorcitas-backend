package cl.duoc.gestorcitas.analitica.messaging.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * Evento leído del tópico {@code gestorcitas.citas.eventos} (lo publica ms-citas, clave = citaId).
 * Las anotaciones definen el contrato mínimo: un registro que no lo cumple se envía al tópico .DLT.
 */
public record CitaEventoMsg(
        @NotBlank String eventoId,
        @NotNull @Pattern(regexp = "CREADA|CONFIRMADA|CANCELADA|ATENDIDA") String tipo,
        @NotNull Long citaId,
        String pacienteId,
        Long medicoId,
        String medicoNombre,
        Long especialidadId,
        String especialidadNombre,
        LocalDateTime fechaHora,
        String estado,
        @NotNull LocalDateTime ocurridoEn
) {
}

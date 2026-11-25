package cl.duoc.gestorcitas.notificaciones.messaging.kafka;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/** Evento leído del tópico Kafka {@code gestorcitas.citas.eventos} (lo publica ms-citas, clave = citaId). */
public record CitaEventoKafkaMsg(
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

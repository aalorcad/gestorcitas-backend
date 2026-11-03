package cl.duoc.gestorcitas.notificaciones.messaging.event;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Evento de cita publicado por ms-citas (exchange topic {@code gestorcitas.citas.eventos}).
 * Las anotaciones definen qué es un mensaje válido: si no se cumplen, el mensaje es irrecuperable y va a la DLQ.
 */
public record CitaEvento(
        @NotBlank String eventoId,
        @NotBlank String tipo,
        @NotNull Long citaId,
        String pacienteId,
        @NotBlank String pacienteNombre,
        @NotBlank @Email String pacienteEmail,
        @NotBlank String medicoNombre,
        String medicoEmail,
        String especialidadNombre,
        @NotNull LocalDateTime fechaHora,
        String estado,
        LocalDateTime ocurridoEn
) {
}

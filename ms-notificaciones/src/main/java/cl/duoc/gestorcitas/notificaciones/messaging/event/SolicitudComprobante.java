package cl.duoc.gestorcitas.notificaciones.messaging.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/** Solicitud de comprobante publicada por ms-citas (exchange direct {@code gestorcitas.citas.comprobantes}). */
public record SolicitudComprobante(
        @NotBlank String solicitudId,
        @NotNull @Positive Long citaId,
        @NotBlank String pacienteNombre,
        String pacienteEmail,
        @NotBlank String medicoNombre,
        String especialidadNombre,
        @NotNull LocalDateTime fechaHora,
        String motivo,
        LocalDateTime solicitadoEn
) {
}

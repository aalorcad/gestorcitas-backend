package cl.duoc.gestorcitas.citas.messaging.event;

import cl.duoc.gestorcitas.citas.entity.Cita;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensaje JSON publicado en el exchange direct de comprobantes.
 * ms-notificaciones lo consume y genera el comprobante (ticket) de la reserva.
 */
public record SolicitudComprobante(
        String solicitudId,
        Long citaId,
        String pacienteNombre,
        String pacienteEmail,
        String medicoNombre,
        String especialidadNombre,
        LocalDateTime fechaHora,
        String motivo,
        LocalDateTime solicitadoEn
) {
    public static SolicitudComprobante desde(Cita cita, LocalDateTime solicitadoEn) {
        return new SolicitudComprobante(
                UUID.randomUUID().toString(),
                cita.getId(),
                cita.getPacienteNombre(),
                cita.getPacienteEmail(),
                cita.getMedicoNombre(),
                cita.getEspecialidadNombre(),
                cita.getFechaHora(),
                cita.getMotivo(),
                solicitadoEn);
    }
}

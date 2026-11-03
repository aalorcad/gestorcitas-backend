package cl.duoc.gestorcitas.citas.messaging.event;

import cl.duoc.gestorcitas.citas.entity.Cita;
import cl.duoc.gestorcitas.citas.messaging.TipoEventoCita;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensaje JSON publicado en el exchange de eventos de citas.
 * Es el contrato con ms-notificaciones: los nombres de los campos no deben cambiar.
 */
public record CitaEvento(
        String eventoId,
        TipoEventoCita tipo,
        Long citaId,
        String pacienteId,
        String pacienteNombre,
        String pacienteEmail,
        String medicoNombre,
        String medicoEmail,
        String especialidadNombre,
        LocalDateTime fechaHora,
        String estado,
        LocalDateTime ocurridoEn
) {
    public static CitaEvento desde(Cita cita, TipoEventoCita tipo, LocalDateTime ocurridoEn) {
        return new CitaEvento(
                UUID.randomUUID().toString(),
                tipo,
                cita.getId(),
                cita.getPacienteId(),
                cita.getPacienteNombre(),
                cita.getPacienteEmail(),
                cita.getMedicoNombre(),
                cita.getMedicoEmail(),
                cita.getEspecialidadNombre(),
                cita.getFechaHora(),
                cita.getEstado() == null ? null : cita.getEstado().name(),
                ocurridoEn);
    }
}

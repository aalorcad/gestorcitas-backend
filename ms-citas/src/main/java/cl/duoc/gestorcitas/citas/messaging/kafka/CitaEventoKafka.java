package cl.duoc.gestorcitas.citas.messaging.kafka;

import cl.duoc.gestorcitas.citas.entity.Cita;
import cl.duoc.gestorcitas.citas.messaging.TipoEventoCita;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensaje JSON publicado en el tópico Kafka {@code gestorcitas.citas.eventos}.
 * <p>
 * Contrato con los consumidores ms-analitica (grupo "analitica") y ms-notificaciones (grupo
 * "historial-citas"). Incluye los identificadores de médico y especialidad que la analítica necesita
 * para agrupar. La clave del registro es {@code citaId}: todos los eventos de una misma cita caen en la
 * misma partición y se leen en orden.
 *
 * @param eventoId identificador único del evento; los consumidores lo usan para descartar duplicados
 */
public record CitaEventoKafka(
        String eventoId,
        TipoEventoCita tipo,
        Long citaId,
        String pacienteId,
        Long medicoId,
        String medicoNombre,
        Long especialidadId,
        String especialidadNombre,
        LocalDateTime fechaHora,
        String estado,
        LocalDateTime ocurridoEn
) {
    public static CitaEventoKafka desde(Cita cita, TipoEventoCita tipo, LocalDateTime ocurridoEn) {
        return new CitaEventoKafka(
                UUID.randomUUID().toString(),
                tipo,
                cita.getId(),
                cita.getPacienteId(),
                cita.getMedicoId(),
                cita.getMedicoNombre(),
                cita.getEspecialidadId(),
                cita.getEspecialidadNombre(),
                cita.getFechaHora(),
                cita.getEstado() == null ? null : cita.getEstado().name(),
                ocurridoEn);
    }
}

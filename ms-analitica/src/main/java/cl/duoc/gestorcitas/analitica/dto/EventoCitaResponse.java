package cl.duoc.gestorcitas.analitica.dto;

import cl.duoc.gestorcitas.analitica.entity.EventoCita;

import java.time.LocalDateTime;

/** Evento de cita procesado, con su ubicación de origen en Kafka (partición y offset). */
public record EventoCitaResponse(
        String eventoId,
        String tipo,
        Long citaId,
        String medicoNombre,
        String especialidadNombre,
        LocalDateTime fechaHora,
        LocalDateTime ocurridoEn,
        int particion,
        long offset,
        LocalDateTime procesadoEn
) {
    public static EventoCitaResponse desde(EventoCita e) {
        return new EventoCitaResponse(e.getEventoId(), e.getTipo(), e.getCitaId(), e.getMedicoNombre(),
                e.getEspecialidadNombre(), e.getFechaHora(), e.getOcurridoEn(), e.getParticion(),
                e.getOffsetKafka(), e.getProcesadoEn());
    }
}

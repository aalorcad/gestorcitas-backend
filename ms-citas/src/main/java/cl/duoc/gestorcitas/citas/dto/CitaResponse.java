package cl.duoc.gestorcitas.citas.dto;

import cl.duoc.gestorcitas.citas.entity.EstadoCita;

import java.time.LocalDateTime;

public record CitaResponse(
        Long id,
        String pacienteId,
        String pacienteNombre,
        String pacienteEmail,
        Long medicoId,
        String medicoNombre,
        Long especialidadId,
        String especialidadNombre,
        LocalDateTime fechaHora,
        String motivo,
        EstadoCita estado,
        String observaciones,
        LocalDateTime creadaEn
) {
}

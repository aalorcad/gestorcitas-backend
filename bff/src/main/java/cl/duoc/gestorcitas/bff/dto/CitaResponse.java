package cl.duoc.gestorcitas.bff.dto;

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
        String estado,
        String observaciones,
        LocalDateTime creadaEn
) {
}

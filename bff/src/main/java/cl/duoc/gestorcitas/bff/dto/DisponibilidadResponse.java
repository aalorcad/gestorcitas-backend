package cl.duoc.gestorcitas.bff.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DisponibilidadResponse(Long medicoId, String medicoNombre, LocalDate fecha,
                                     List<LocalDateTime> horariosDisponibles) {
}

package cl.duoc.gestorcitas.citas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/**
 * Parámetros de la agenda médica (configurables en application.yml).
 * Las horas se reciben como texto "HH:mm".
 */
@ConfigurationProperties(prefix = "app.agenda")
public record AgendaProperties(
        String horaInicio,
        String horaFin,
        int duracionMinutos,
        Set<DayOfWeek> diasHabiles,
        int maxCitasActivasPorEspecialidad
) {
    public AgendaProperties {
        if (horaInicio == null || horaInicio.isBlank()) horaInicio = "08:00";
        if (horaFin == null || horaFin.isBlank()) horaFin = "18:00";
        if (duracionMinutos <= 0) duracionMinutos = 30;
        if (diasHabiles == null || diasHabiles.isEmpty()) {
            diasHabiles = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        }
        if (maxCitasActivasPorEspecialidad <= 0) maxCitasActivasPorEspecialidad = 1;
    }

    public LocalTime inicio() {
        return LocalTime.parse(horaInicio);
    }

    public LocalTime fin() {
        return LocalTime.parse(horaFin);
    }
}

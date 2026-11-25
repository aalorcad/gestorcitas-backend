package cl.duoc.gestorcitas.analitica.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Indicadores calculados a partir del streaming de Kafka.
 *
 * @param totalEventos            eventos de cita procesados (sin duplicados)
 * @param eventosPorTipo          CREADA / CONFIRMADA / CANCELADA / ATENDIDA
 * @param reservasPorEspecialidad citas creadas agrupadas por especialidad
 * @param reservasPorMedico       citas creadas agrupadas por médico
 * @param tasaCancelacion         % de cancelaciones sobre citas creadas (1 decimal)
 * @param usuarios                resumen del estado de usuarios (tópico compactado)
 */
public record ResumenAnaliticaResponse(
        long totalEventos,
        Map<String, Long> eventosPorTipo,
        List<ConteoResponse> reservasPorEspecialidad,
        List<ConteoResponse> reservasPorMedico,
        double tasaCancelacion,
        UsuariosResumen usuarios,
        LocalDateTime generadoEn
) {
    public record UsuariosResumen(long total, long activos, long inactivos, Map<String, Long> porRol) {
    }
}

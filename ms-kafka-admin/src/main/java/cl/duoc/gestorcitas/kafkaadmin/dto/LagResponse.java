package cl.duoc.gestorcitas.kafkaadmin.dto;

/**
 * Retraso (lag) del grupo en una partición.
 *
 * @param offsetConfirmado último offset confirmado por el grupo (próximo a leer)
 * @param offsetFinal      offset del próximo registro que se escribirá en la partición
 * @param lag              registros pendientes de procesar = offsetFinal - offsetConfirmado
 */
public record LagResponse(String topico, int particion, long offsetConfirmado, long offsetFinal, long lag) {
}

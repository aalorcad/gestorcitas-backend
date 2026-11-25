package cl.duoc.gestorcitas.kafkaadmin.dto;

import java.util.List;

/**
 * Partición de un tópico.
 *
 * @param lider        broker líder (null si la partición no tiene líder disponible)
 * @param replicas     brokers que guardan una copia
 * @param isr          réplicas en sincronía (in-sync replicas)
 * @param offsetInicial primer offset disponible (los anteriores se borraron por retención)
 * @param offsetFinal   offset del próximo registro que se escribirá
 */
public record ParticionResponse(int particion, Integer lider, List<Integer> replicas, List<Integer> isr,
                                long offsetInicial, long offsetFinal) {
}

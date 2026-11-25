package cl.duoc.gestorcitas.kafkaadmin.dto;

/** Tópico en el listado. */
public record TopicoResumenResponse(String nombre, int particiones, int factorReplicacion, boolean protegido) {
}

package cl.duoc.gestorcitas.kafkaadmin.dto;

/** Grupo de consumidores en el listado (estado: STABLE, EMPTY, PREPARING_REBALANCE, ...). */
public record GrupoResumenResponse(String grupo, String estado) {
}

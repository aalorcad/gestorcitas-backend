package cl.duoc.gestorcitas.kafkaadmin.dto;

import java.util.List;

/** Detalle de un grupo de consumidores: miembros, asignaciones y lag por partición. */
public record GrupoDetalleResponse(String grupo, String estado, Integer coordinador, List<MiembroResponse> miembros,
                                   List<LagResponse> lag, long lagTotal) {
}

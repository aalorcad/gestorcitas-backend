package cl.duoc.gestorcitas.kafkaadmin.dto;

import java.util.List;

/** Consumidor activo del grupo y las particiones que tiene asignadas ("tópico-partición"). */
public record MiembroResponse(String consumerId, String clientId, String host, List<String> asignaciones) {
}

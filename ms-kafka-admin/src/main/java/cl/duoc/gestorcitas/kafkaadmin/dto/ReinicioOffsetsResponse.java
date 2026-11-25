package cl.duoc.gestorcitas.kafkaadmin.dto;

import java.util.Map;

/** Resultado del reinicio: nuevo offset confirmado por partición. */
public record ReinicioOffsetsResponse(String grupo, String topico, EstrategiaReinicio estrategia,
                                      Map<Integer, Long> nuevosOffsets) {
}

package cl.duoc.gestorcitas.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

/**
 * Detalle de un tópico.
 *
 * @param configuracion solo las configuraciones definidas para el tópico (las que difieren del valor del broker)
 * @param registros     suma de (offsetFinal - offsetInicial) de todas las particiones. En un tópico compactado
 *                      es una cota superior: la compactación deja huecos entre offsets.
 */
public record TopicoDetalleResponse(String nombre, boolean protegido, int factorReplicacion,
                                    List<ParticionResponse> particiones, Map<String, String> configuracion,
                                    long registros) {
}

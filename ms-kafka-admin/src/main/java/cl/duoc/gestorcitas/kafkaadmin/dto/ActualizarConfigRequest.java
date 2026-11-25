package cl.duoc.gestorcitas.kafkaadmin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * Cambios de configuración de un tópico, p. ej. {@code {"retention.ms": "86400000"}}.
 * Un valor vacío ("") elimina la configuración del tópico y vuelve al valor por defecto del broker.
 */
public record ActualizarConfigRequest(@NotEmpty(message = "debe indicar al menos una configuración")
                                      Map<String, String> configuracion) {
}

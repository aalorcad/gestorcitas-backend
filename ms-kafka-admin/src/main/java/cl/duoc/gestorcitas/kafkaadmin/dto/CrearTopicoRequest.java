package cl.duoc.gestorcitas.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Petición para crear un tópico.
 *
 * @param nombre            nombre del tópico
 * @param particiones       1 a 50
 * @param factorReplicacion 1 a 3 (no puede superar la cantidad de brokers)
 * @param configuracion     opcional; solo claves de {@code app.admin.configuraciones-permitidas}
 */
public record CrearTopicoRequest(
        @NombreTopico String nombre,
        @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 50, message = "no puede superar 50") Integer particiones,
        @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 3, message = "no puede superar 3") Integer factorReplicacion,
        Map<String, String> configuracion
) {
}

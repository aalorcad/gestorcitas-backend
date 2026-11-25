package cl.duoc.gestorcitas.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nuevo total de particiones. Kafka solo permite AUMENTARLAS (nunca reducirlas). */
public record AumentarParticionesRequest(
        @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 50, message = "no puede superar 50") Integer total) {
}

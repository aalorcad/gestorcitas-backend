package cl.duoc.gestorcitas.rabbitadmin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Petición para crear un exchange.
 *
 * @param nombre     nombre del exchange (obligatorio)
 * @param tipo       direct | topic | fanout | headers (obligatorio)
 * @param durable    sobrevive a reinicios del broker (por defecto true)
 * @param autoDelete se elimina cuando pierde su último binding (por defecto false)
 */
public record CrearExchangeRequest(
        @NombreAmqp String nombre,
        @NotNull(message = "es obligatorio (direct, topic, fanout o headers)") TipoExchange tipo,
        Boolean durable,
        Boolean autoDelete
) {
    public boolean esDurable() {
        return durable == null || durable;
    }

    public boolean esAutoDelete() {
        return autoDelete != null && autoDelete;
    }
}

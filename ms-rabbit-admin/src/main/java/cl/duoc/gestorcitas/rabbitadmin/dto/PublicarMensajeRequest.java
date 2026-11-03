package cl.duoc.gestorcitas.rabbitadmin.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Publicación de un mensaje de prueba (para demostrar enrutamiento, reintentos y DLQ).
 *
 * @param exchange   exchange destino (debe existir)
 * @param routingKey routing key del mensaje (sin comodines)
 * @param payload    contenido JSON del mensaje
 */
public record PublicarMensajeRequest(
        @NombreAmqp String exchange,
        @NotNull(message = "es obligatoria")
        @Size(max = ReglasAmqp.LARGO_MAXIMO, message = "no puede superar " + ReglasAmqp.LARGO_MAXIMO + " caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._:-]*$", message = "solo admite letras, números, '.', '_', ':' y '-' (sin comodines)")
        String routingKey,
        @NotNull(message = "es obligatorio") JsonNode payload
) {
}

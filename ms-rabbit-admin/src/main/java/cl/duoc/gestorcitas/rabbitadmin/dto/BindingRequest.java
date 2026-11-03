package cl.duoc.gestorcitas.rabbitadmin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Enlace entre un exchange y una cola.
 * Se recibe como JSON al crear ({@code POST /bindings}) y como query params al eliminar ({@code DELETE /bindings}).
 *
 * @param exchange   exchange de origen
 * @param cola       cola de destino
 * @param routingKey clave o patrón de enrutamiento ('*' y '#' solo en exchanges topic). Puede ser vacía.
 */
public record BindingRequest(
        @NombreAmqp String exchange,
        @NombreAmqp String cola,
        @NotNull(message = "es obligatoria (usa \"\" si el exchange es fanout)")
        @Size(max = ReglasAmqp.LARGO_MAXIMO, message = "no puede superar " + ReglasAmqp.LARGO_MAXIMO + " caracteres")
        @Pattern(regexp = ReglasAmqp.PATRON_ROUTING_KEY, message = ReglasAmqp.MENSAJE_ROUTING_KEY)
        String routingKey
) {
    public boolean usaComodines() {
        return routingKey != null && (routingKey.contains("*") || routingKey.contains("#"));
    }
}

package cl.duoc.gestorcitas.rabbitadmin.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Binding según GET /api/bindings de la API de management. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagementBinding(
        String source,
        String destination,
        @JsonProperty("destination_type") String destinationType,
        @JsonProperty("routing_key") String routingKey
) {
    /** Binding de un exchange real hacia una cola (excluye los bindings implícitos del exchange por defecto). */
    public boolean esDeColaExplicito() {
        return "queue".equals(destinationType) && source != null && !source.isEmpty();
    }
}

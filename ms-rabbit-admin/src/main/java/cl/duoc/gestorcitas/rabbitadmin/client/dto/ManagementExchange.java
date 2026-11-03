package cl.duoc.gestorcitas.rabbitadmin.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Exchange según GET /api/exchanges de la API de management. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagementExchange(
        String name,
        String type,
        Boolean durable,
        @JsonProperty("auto_delete") Boolean autoDelete
) {
    /** El exchange por defecto ("") y los "amq.*" son del sistema y no se administran. */
    public boolean esDelSistema() {
        return name == null || name.isEmpty() || name.startsWith("amq.");
    }
}

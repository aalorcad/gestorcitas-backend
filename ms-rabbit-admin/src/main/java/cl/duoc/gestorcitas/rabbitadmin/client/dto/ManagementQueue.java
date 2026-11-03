package cl.duoc.gestorcitas.rabbitadmin.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/** Cola según GET /api/queues de la API de management. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagementQueue(
        String name,
        Boolean durable,
        @JsonProperty("auto_delete") Boolean autoDelete,
        Long messages,
        Integer consumers,
        Map<String, Object> arguments
) {
}

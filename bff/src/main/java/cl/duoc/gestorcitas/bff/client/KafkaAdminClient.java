package cl.duoc.gestorcitas.bff.client;

import cl.duoc.gestorcitas.bff.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;

/**
 * Acceso HTTP a ms-kafka-admin (red privada).
 * El BFF no reinterpreta los datos: reenvía el JSON tal cual y propaga los errores (400/404/409/503)
 * con su mensaje original, porque la validación y las reglas viven en ms-kafka-admin.
 * Las rutas se arman con plantillas URI ({@code /topics/{nombre}}) para que los valores se codifiquen.
 */
@Component
public class KafkaAdminClient {

    private final RestClient client;

    public KafkaAdminClient(@Qualifier("kafkaAdminRestClient") RestClient client) {
        this.client = client;
    }

    public JsonNode get(String plantilla, Object... variables) {
        return call(() -> client.get().uri(plantilla, variables)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(JsonNode.class));
    }

    public JsonNode post(String plantilla, JsonNode body, Object... variables) {
        return call(() -> client.post().uri(plantilla, variables).body(body)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(JsonNode.class));
    }

    public JsonNode patch(String plantilla, JsonNode body, Object... variables) {
        return call(() -> client.patch().uri(plantilla, variables).body(body)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(JsonNode.class));
    }

    /** DELETE sin cuerpo de respuesta (204). */
    public void delete(String plantilla, Object... variables) {
        call(() -> client.delete().uri(plantilla, variables)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .toBodilessEntity());
    }

    private <T> T call(Supplier<T> s) {
        try {
            return s.get();
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("ms-kafka-admin no disponible", ex);
        }
    }
}

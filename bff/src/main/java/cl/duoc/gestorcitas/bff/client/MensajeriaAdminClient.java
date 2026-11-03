package cl.duoc.gestorcitas.bff.client;

import cl.duoc.gestorcitas.bff.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Acceso HTTP a ms-rabbit-admin (red privada).
 * El BFF no reinterpreta los datos: reenvía el JSON tal cual y propaga los errores (400/404/409/503)
 * con su mensaje original, porque la validación y las reglas viven en ms-rabbit-admin.
 * Las rutas se arman con plantillas URI ({@code /queues/{nombre}}) para que los valores se codifiquen.
 */
@Component
public class MensajeriaAdminClient {

    private final RestClient client;

    public MensajeriaAdminClient(@Qualifier("mensajeriaAdminRestClient") RestClient client) {
        this.client = client;
    }

    public JsonNode get(String plantilla, Object... variables) {
        return call(() -> client.get().uri(plantilla, variables)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(JsonNode.class));
    }

    public JsonNode post(String ruta, JsonNode body) {
        return call(() -> client.post().uri(ruta).body(body)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(JsonNode.class));
    }

    /** DELETE sin cuerpo de respuesta (204). */
    public void delete(String plantilla, Map<String, String> queryParams, Object... variables) {
        call(() -> client.delete()
                .uri(u -> {
                    u.path(plantilla);
                    queryParams.forEach(u::queryParam);
                    return u.build(variables);
                })
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .toBodilessEntity());
    }

    /** DELETE que devuelve un cuerpo JSON (por ejemplo, el resultado de purgar una cola). */
    public JsonNode deleteConRespuesta(String plantilla, Object... variables) {
        return call(() -> client.delete().uri(plantilla, variables)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(JsonNode.class));
    }

    private <T> T call(Supplier<T> s) {
        try {
            return s.get();
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("ms-rabbit-admin no disponible", ex);
        }
    }
}

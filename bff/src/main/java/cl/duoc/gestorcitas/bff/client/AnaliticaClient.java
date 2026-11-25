package cl.duoc.gestorcitas.bff.client;

import cl.duoc.gestorcitas.bff.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Acceso HTTP a ms-analitica (red privada). Reenvía el JSON de los indicadores tal cual. */
@Component
public class AnaliticaClient {

    private final RestClient client;

    public AnaliticaClient(@Qualifier("analiticaRestClient") RestClient client) {
        this.client = client;
    }

    public JsonNode resumen() {
        return get("/analitica/resumen");
    }

    public JsonNode ultimosEventos(int limite) {
        return get("/analitica/eventos?limite={limite}", limite);
    }

    private JsonNode get(String plantilla, Object... variables) {
        try {
            return client.get().uri(plantilla, variables)
                    .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                    .body(JsonNode.class);
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("ms-analitica no disponible", ex);
        }
    }
}

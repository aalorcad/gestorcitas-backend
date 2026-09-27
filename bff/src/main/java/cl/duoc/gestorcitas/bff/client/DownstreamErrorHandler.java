package cl.duoc.gestorcitas.bff.client;

import cl.duoc.gestorcitas.bff.exception.DownstreamException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Traduce respuestas 4xx/5xx de los microservicios a {@link DownstreamException}. */
final class DownstreamErrorHandler {

    private DownstreamErrorHandler() {
    }

    static RestClient.ResponseSpec.ErrorHandler handler() {
        return (request, response) -> {
            throw new DownstreamException(response.getStatusCode(), leer(response));
        };
    }

    static boolean esError(HttpStatusCode status) {
        return status.isError();
    }

    private static String leer(ClientHttpResponse response) throws IOException {
        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        return body.isBlank() ? "{\"message\":\"Error en microservicio\"}" : body;
    }
}

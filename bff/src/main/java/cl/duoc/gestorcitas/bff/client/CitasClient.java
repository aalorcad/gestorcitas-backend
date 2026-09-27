package cl.duoc.gestorcitas.bff.client;

import cl.duoc.gestorcitas.bff.dto.*;
import cl.duoc.gestorcitas.bff.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * Acceso HTTP a ms-citas (red privada). Propaga la identidad del usuario ya validada
 * por el BFF mediante cabeceras X-User-*.
 */
@Component
public class CitasClient {

    private final RestClient client;

    public CitasClient(@Qualifier("citasRestClient") RestClient client) {
        this.client = client;
    }

    public CitaResponse reservar(CrearCitaRequest body, UsuarioAutenticado u) {
        return call(() -> client.post().uri("/citas").headers(UsuarioHeaders.de(u)).body(body)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(CitaResponse.class));
    }

    public List<CitaResponse> misCitas(UsuarioAutenticado u) {
        return call(() -> client.get().uri("/citas/mias").headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(new ParameterizedTypeReference<List<CitaResponse>>() { }));
    }

    public CitaResponse cancelar(Long id, UsuarioAutenticado u) {
        return patch("/citas/{id}/cancelar", id, null, u);
    }

    public List<CitaResponse> agenda(LocalDate fecha, UsuarioAutenticado u) {
        return call(() -> client.get()
                .uri(b -> {
                    b.path("/citas/agenda");
                    if (fecha != null) {
                        b.queryParam("fecha", fecha);
                    }
                    return b.build();
                })
                .headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(new ParameterizedTypeReference<List<CitaResponse>>() { }));
    }

    public CitaResponse confirmar(Long id, UsuarioAutenticado u) {
        return patch("/citas/{id}/confirmar", id, null, u);
    }

    public CitaResponse atender(Long id, AtenderCitaRequest body, UsuarioAutenticado u) {
        return patch("/citas/{id}/atender", id, body, u);
    }

    public List<CitaResponse> todas(String estado, UsuarioAutenticado u) {
        return call(() -> client.get()
                .uri(b -> {
                    b.path("/citas");
                    if (estado != null && !estado.isBlank()) {
                        b.queryParam("estado", estado);
                    }
                    return b.build();
                })
                .headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(new ParameterizedTypeReference<List<CitaResponse>>() { }));
    }

    public ResumenCitasResponse resumen(UsuarioAutenticado u) {
        return call(() -> client.get().uri("/citas/resumen").headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(ResumenCitasResponse.class));
    }

    public DisponibilidadResponse disponibilidad(Long medicoId, LocalDate fecha, UsuarioAutenticado u) {
        return call(() -> client.get()
                .uri(b -> b.path("/citas/disponibilidad")
                        .queryParam("medicoId", medicoId)
                        .queryParam("fecha", fecha)
                        .build())
                .headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(DisponibilidadResponse.class));
    }

    private CitaResponse patch(String uri, Long id, Object body, UsuarioAutenticado u) {
        return call(() -> {
            RestClient.RequestBodySpec spec = client.patch().uri(uri, id).headers(UsuarioHeaders.de(u));
            if (body != null) {
                spec.body(body);
            }
            return spec.retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                    .body(CitaResponse.class);
        });
    }

    private <T> T call(Supplier<T> s) {
        try {
            return s.get();
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("ms-citas no disponible", ex);
        }
    }
}

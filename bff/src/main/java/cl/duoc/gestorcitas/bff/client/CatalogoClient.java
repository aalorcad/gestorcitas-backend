package cl.duoc.gestorcitas.bff.client;

import cl.duoc.gestorcitas.bff.dto.EspecialidadRequest;
import cl.duoc.gestorcitas.bff.dto.EspecialidadResponse;
import cl.duoc.gestorcitas.bff.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.function.Supplier;

/** Acceso HTTP a ms-catalogo (red privada). */
@Component
public class CatalogoClient {

    private final RestClient client;

    public CatalogoClient(@Qualifier("catalogoRestClient") RestClient client) {
        this.client = client;
    }

    public List<EspecialidadResponse> listarEspecialidades(boolean soloActivas) {
        return call(() -> client.get()
                .uri(u -> u.path("/especialidades").queryParam("soloActivas", soloActivas).build())
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(new ParameterizedTypeReference<List<EspecialidadResponse>>() { }));
    }

    public EspecialidadResponse crearEspecialidad(EspecialidadRequest body) {
        return call(() -> client.post().uri("/especialidades").body(body)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(EspecialidadResponse.class));
    }

    public EspecialidadResponse actualizarEspecialidad(Long id, EspecialidadRequest body) {
        return call(() -> client.put().uri("/especialidades/{id}", id).body(body)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(EspecialidadResponse.class));
    }

    public EspecialidadResponse activarEspecialidad(Long id) {
        return call(() -> client.patch().uri("/especialidades/{id}/activar", id)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(EspecialidadResponse.class));
    }

    public void desactivarEspecialidad(Long id) {
        call(() -> client.delete().uri("/especialidades/{id}", id)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .toBodilessEntity());
    }

    private <T> T call(Supplier<T> s) {
        try {
            return s.get();
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("ms-catalogo no disponible", ex);
        }
    }
}

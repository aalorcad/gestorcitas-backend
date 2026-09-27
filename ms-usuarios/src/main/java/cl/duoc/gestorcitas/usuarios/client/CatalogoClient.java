package cl.duoc.gestorcitas.usuarios.client;

import cl.duoc.gestorcitas.usuarios.dto.EspecialidadDto;
import cl.duoc.gestorcitas.usuarios.exception.ExternalServiceException;
import cl.duoc.gestorcitas.usuarios.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** Cliente HTTP hacia ms-catalogo (solo comunicación). */
@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(@Qualifier("catalogoRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public EspecialidadDto obtenerEspecialidad(Long id) {
        try {
            return restClient.get().uri("/especialidades/{id}", id).retrieve().body(EspecialidadDto.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Especialidad " + id + " no encontrada");
            }
            throw new ExternalServiceException("Error consultando el catálogo: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Catálogo no disponible", ex);
        }
    }

    public List<EspecialidadDto> listarEspecialidades() {
        try {
            List<EspecialidadDto> lista = restClient.get()
                    .uri(u -> u.path("/especialidades").queryParam("soloActivas", false).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EspecialidadDto>>() { });
            return lista == null ? List.of() : lista;
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Catálogo no disponible", ex);
        }
    }
}

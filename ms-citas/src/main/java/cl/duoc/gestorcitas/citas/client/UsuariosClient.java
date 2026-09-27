package cl.duoc.gestorcitas.citas.client;

import cl.duoc.gestorcitas.citas.dto.MedicoDto;
import cl.duoc.gestorcitas.citas.dto.PacienteEstadoDto;
import cl.duoc.gestorcitas.citas.exception.ExternalServiceException;
import cl.duoc.gestorcitas.citas.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente HTTP hacia ms-usuarios (solo comunicación, sin reglas de negocio). */
@Component
public class UsuariosClient {

    private final RestClient restClient;

    public UsuariosClient(@Qualifier("usuariosRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public MedicoDto obtenerMedico(Long medicoId) {
        try {
            return restClient.get().uri("/medicos/{id}", medicoId).retrieve().body(MedicoDto.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Médico " + medicoId + " no encontrado");
            }
            throw new ExternalServiceException("Error consultando usuarios: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Servicio de usuarios no disponible", ex);
        }
    }

    public PacienteEstadoDto estadoPaciente(String oid) {
        try {
            return restClient.get().uri("/usuarios/pacientes/{oid}/estado", oid).retrieve().body(PacienteEstadoDto.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Servicio de usuarios no disponible", ex);
        }
    }
}

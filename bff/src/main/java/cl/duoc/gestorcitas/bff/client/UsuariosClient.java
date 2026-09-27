package cl.duoc.gestorcitas.bff.client;

import cl.duoc.gestorcitas.bff.dto.*;
import cl.duoc.gestorcitas.bff.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.function.Supplier;

/** Acceso HTTP a ms-usuarios (red privada). */
@Component
public class UsuariosClient {

    private final RestClient client;

    public UsuariosClient(@Qualifier("usuariosRestClient") RestClient client) {
        this.client = client;
    }

    // ---- Usuario autenticado
    public UsuarioResponse sincronizar(UsuarioAutenticado u) {
        return call(() -> client.post().uri("/usuarios/sincronizar").headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(UsuarioResponse.class));
    }

    public UsuarioResponse me(UsuarioAutenticado u) {
        return call(() -> client.get().uri("/usuarios/me").headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(UsuarioResponse.class));
    }

    public UsuarioResponse actualizarPerfilPaciente(ActualizarPerfilPacienteRequest body, UsuarioAutenticado u) {
        return call(() -> client.put().uri("/usuarios/me/perfil-paciente").headers(UsuarioHeaders.de(u)).body(body)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(UsuarioResponse.class));
    }

    public UsuarioResponse actualizarPerfilMedico(ActualizarPerfilMedicoRequest body, UsuarioAutenticado u) {
        return call(() -> client.put().uri("/usuarios/me/perfil-medico").headers(UsuarioHeaders.de(u)).body(body)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(UsuarioResponse.class));
    }

    // ---- Administración
    public List<UsuarioResponse> listar(String rol, String q, UsuarioAutenticado u) {
        return call(() -> client.get()
                .uri(b -> {
                    b.path("/usuarios");
                    if (rol != null && !rol.isBlank()) b.queryParam("rol", rol);
                    if (q != null && !q.isBlank()) b.queryParam("q", q);
                    return b.build();
                })
                .headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(new ParameterizedTypeReference<List<UsuarioResponse>>() { }));
    }

    public ResumenUsuariosResponse resumen(UsuarioAutenticado u) {
        return call(() -> client.get().uri("/usuarios/resumen").headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(ResumenUsuariosResponse.class));
    }

    public UsuarioResponse activar(Long id, UsuarioAutenticado u) {
        return call(() -> client.patch().uri("/usuarios/{id}/activar", id).headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(UsuarioResponse.class));
    }

    public UsuarioResponse desactivar(Long id, UsuarioAutenticado u) {
        return call(() -> client.patch().uri("/usuarios/{id}/desactivar", id).headers(UsuarioHeaders.de(u))
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(UsuarioResponse.class));
    }

    // ---- Médicos
    public List<MedicoResponse> listarMedicos(Long especialidadId) {
        return call(() -> client.get()
                .uri(b -> {
                    b.path("/medicos");
                    if (especialidadId != null) b.queryParam("especialidadId", especialidadId);
                    return b.build();
                })
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(new ParameterizedTypeReference<List<MedicoResponse>>() { }));
    }

    public long contarMedicosActivos(Long especialidadId) {
        Long n = call(() -> client.get()
                .uri(b -> b.path("/medicos/conteo").queryParam("especialidadId", especialidadId).build())
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(Long.class));
        return n == null ? 0 : n;
    }

    public UsuarioResponse registrarMedico(RegistrarMedicoRequest body, UsuarioAutenticado u) {
        return call(() -> client.post().uri("/medicos").headers(UsuarioHeaders.de(u)).body(body)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(UsuarioResponse.class));
    }

    public UsuarioResponse asignarPerfilMedico(Long id, AsignarPerfilMedicoRequest body, UsuarioAutenticado u) {
        return call(() -> client.put().uri("/medicos/{id}/perfil", id).headers(UsuarioHeaders.de(u)).body(body)
                .retrieve().onStatus(DownstreamErrorHandler::esError, DownstreamErrorHandler.handler())
                .body(UsuarioResponse.class));
    }

    private <T> T call(Supplier<T> s) {
        try {
            return s.get();
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("ms-usuarios no disponible", ex);
        }
    }
}

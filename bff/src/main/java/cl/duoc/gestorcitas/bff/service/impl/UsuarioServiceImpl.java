package cl.duoc.gestorcitas.bff.service.impl;

import cl.duoc.gestorcitas.bff.client.CatalogoClient;
import cl.duoc.gestorcitas.bff.client.CitasClient;
import cl.duoc.gestorcitas.bff.client.UsuariosClient;
import cl.duoc.gestorcitas.bff.dto.*;
import cl.duoc.gestorcitas.bff.service.IdentidadService;
import cl.duoc.gestorcitas.bff.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

/** Orquesta ms-usuarios propagando la identidad validada del JWT. */
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuariosClient usuariosClient;
    private final CitasClient citasClient;
    private final CatalogoClient catalogoClient;
    private final IdentidadService identidadService;

    @Override
    public UsuarioResponse sincronizar(Jwt jwt) {
        return usuariosClient.sincronizar(identidadService.desdeToken(jwt));
    }

    @Override
    public UsuarioResponse me(Jwt jwt) {
        return usuariosClient.me(identidadService.desdeToken(jwt));
    }

    @Override
    public UsuarioResponse actualizarPerfilPaciente(ActualizarPerfilPacienteRequest request, Jwt jwt) {
        return usuariosClient.actualizarPerfilPaciente(request, identidadService.desdeToken(jwt));
    }

    @Override
    public UsuarioResponse actualizarPerfilMedico(ActualizarPerfilMedicoRequest request, Jwt jwt) {
        return usuariosClient.actualizarPerfilMedico(request, identidadService.desdeToken(jwt));
    }

    @Override
    public List<MedicoResponse> listarMedicos(Long especialidadId) {
        return usuariosClient.listarMedicos(especialidadId);
    }

    @Override
    public List<UsuarioResponse> listar(String rol, String q, Jwt jwt) {
        return usuariosClient.listar(rol, q, identidadService.desdeToken(jwt));
    }

    @Override
    public UsuarioResponse activar(Long id, Jwt jwt) {
        return usuariosClient.activar(id, identidadService.desdeToken(jwt));
    }

    @Override
    public UsuarioResponse desactivar(Long id, Jwt jwt) {
        return usuariosClient.desactivar(id, identidadService.desdeToken(jwt));
    }

    @Override
    public UsuarioResponse registrarMedico(RegistrarMedicoRequest request, Jwt jwt) {
        return usuariosClient.registrarMedico(request, identidadService.desdeToken(jwt));
    }

    @Override
    public UsuarioResponse asignarPerfilMedico(Long id, AsignarPerfilMedicoRequest request, Jwt jwt) {
        return usuariosClient.asignarPerfilMedico(id, request, identidadService.desdeToken(jwt));
    }

    /** Compone indicadores de ms-usuarios, ms-citas y ms-catalogo en una sola respuesta. */
    @Override
    public DashboardAdminResponse dashboard(Jwt jwt) {
        UsuarioAutenticado u = identidadService.desdeToken(jwt);
        return new DashboardAdminResponse(
                usuariosClient.resumen(u),
                citasClient.resumen(u),
                catalogoClient.listarEspecialidades(true).size());
    }
}

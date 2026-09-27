package cl.duoc.gestorcitas.bff.service;

import cl.duoc.gestorcitas.bff.dto.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface UsuarioService {

    UsuarioResponse sincronizar(Jwt jwt);

    UsuarioResponse me(Jwt jwt);

    UsuarioResponse actualizarPerfilPaciente(ActualizarPerfilPacienteRequest request, Jwt jwt);

    UsuarioResponse actualizarPerfilMedico(ActualizarPerfilMedicoRequest request, Jwt jwt);

    List<MedicoResponse> listarMedicos(Long especialidadId);

    List<UsuarioResponse> listar(String rol, String q, Jwt jwt);

    UsuarioResponse activar(Long id, Jwt jwt);

    UsuarioResponse desactivar(Long id, Jwt jwt);

    UsuarioResponse registrarMedico(RegistrarMedicoRequest request, Jwt jwt);

    UsuarioResponse asignarPerfilMedico(Long id, AsignarPerfilMedicoRequest request, Jwt jwt);

    DashboardAdminResponse dashboard(Jwt jwt);
}

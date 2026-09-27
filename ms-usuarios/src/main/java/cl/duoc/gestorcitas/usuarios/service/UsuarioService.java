package cl.duoc.gestorcitas.usuarios.service;

import cl.duoc.gestorcitas.usuarios.dto.*;

import java.util.List;

public interface UsuarioService {

    /** Alta/actualización del usuario autenticado a partir de su identidad Entra ID. */
    UsuarioResponse sincronizar(UsuarioContexto ctx);

    UsuarioResponse obtenerActual(UsuarioContexto ctx);

    UsuarioResponse actualizarPerfilPaciente(ActualizarPerfilPacienteRequest request, UsuarioContexto ctx);

    UsuarioResponse actualizarPerfilMedico(ActualizarPerfilMedicoRequest request, UsuarioContexto ctx);

    // ---- Administración
    List<UsuarioResponse> listar(String rol, String q, UsuarioContexto ctx);

    UsuarioResponse obtener(Long id, UsuarioContexto ctx);

    UsuarioResponse activar(Long id, UsuarioContexto ctx);

    UsuarioResponse desactivar(Long id, UsuarioContexto ctx);

    ResumenUsuariosResponse resumen(UsuarioContexto ctx);

    // ---- Consumido por ms-citas
    PacienteEstadoResponse estadoPaciente(String oid);
}

package cl.duoc.gestorcitas.usuarios.service;

import cl.duoc.gestorcitas.usuarios.dto.*;

import java.util.List;

public interface MedicoService {

    /** Médicos activos con especialidad asignada (para reservar). */
    List<MedicoResponse> listarAgendables(Long especialidadId);

    MedicoResponse obtener(Long id);

    /** Admin pre-registra un médico. */
    UsuarioResponse registrar(RegistrarMedicoRequest request, UsuarioContexto ctx);

    /** Admin actualiza los datos profesionales de un médico. */
    UsuarioResponse asignarPerfil(Long usuarioId, AsignarPerfilMedicoRequest request, UsuarioContexto ctx);

    /** Cantidad de médicos activos de una especialidad (regla de catálogo). */
    long contarActivosPorEspecialidad(Long especialidadId);
}

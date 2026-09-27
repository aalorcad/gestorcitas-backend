package cl.duoc.gestorcitas.usuarios.service.impl;

import cl.duoc.gestorcitas.usuarios.dto.*;
import cl.duoc.gestorcitas.usuarios.entity.PerfilMedico;
import cl.duoc.gestorcitas.usuarios.entity.RolUsuario;
import cl.duoc.gestorcitas.usuarios.entity.Usuario;
import cl.duoc.gestorcitas.usuarios.exception.BusinessException;
import cl.duoc.gestorcitas.usuarios.exception.ForbiddenOperationException;
import cl.duoc.gestorcitas.usuarios.exception.ResourceNotFoundException;
import cl.duoc.gestorcitas.usuarios.mapper.UsuarioMapper;
import cl.duoc.gestorcitas.usuarios.repository.UsuarioRepository;
import cl.duoc.gestorcitas.usuarios.service.MedicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Reglas de negocio específicas de los médicos. */
@Service
@RequiredArgsConstructor
public class MedicoServiceImpl implements MedicoService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper mapper;
    private final EspecialidadResolver especialidades;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<MedicoResponse> listarAgendables(Long especialidadId) {
        List<Usuario> medicos = especialidadId == null
                ? usuarioRepository.findMedicosAgendables()
                : usuarioRepository.findMedicosAgendablesPorEspecialidad(especialidadId);
        Map<Long, String> nombres = especialidades.nombres();
        return medicos.stream()
                .map(u -> mapper.toMedicoResponse(u, nombres.get(u.getPerfilMedico().getEspecialidadId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MedicoResponse obtener(Long id) {
        Usuario u = usuarioRepository.findById(id)
                .filter(x -> x.tieneRol(RolUsuario.MEDICO))
                .orElseThrow(() -> new ResourceNotFoundException("Médico " + id + " no encontrado"));
        Long espId = u.getPerfilMedico() == null ? null : u.getPerfilMedico().getEspecialidadId();
        String nombreEsp = espId == null ? null : especialidades.nombres().get(espId);
        return mapper.toMedicoResponse(u, nombreEsp);
    }

    @Override
    @Transactional
    public UsuarioResponse registrar(RegistrarMedicoRequest request, UsuarioContexto ctx) {
        exigirAdmin(ctx);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Ya existe un usuario con el email " + email);
        }
        EspecialidadDto esp = especialidades.exigirActiva(request.especialidadId());

        Usuario medico = Usuario.builder()
                .email(email)
                .nombre(request.nombre().trim())
                .telefono(StringUtils.hasText(request.telefono()) ? request.telefono().trim() : null)
                .activo(true)
                .roles(EnumSet.of(RolUsuario.MEDICO))
                .perfilMedico(PerfilMedico.builder()
                        .especialidadId(esp.id())
                        .registroProfesional(request.registroProfesional().trim())
                        .build())
                .creadoEn(LocalDateTime.now(clock))
                .build();
        Usuario guardado = usuarioRepository.save(medico);
        return mapper.toResponse(guardado, Map.of(esp.id(), esp.nombre()));
    }

    @Override
    @Transactional
    public UsuarioResponse asignarPerfil(Long usuarioId, AsignarPerfilMedicoRequest request, UsuarioContexto ctx) {
        exigirAdmin(ctx);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + usuarioId + " no encontrado"));
        if (!usuario.tieneRol(RolUsuario.MEDICO)) {
            throw new BusinessException("El usuario no tiene el rol Medico");
        }
        EspecialidadDto esp = especialidades.exigirActiva(request.especialidadId());

        PerfilMedico perfil = Optional.ofNullable(usuario.getPerfilMedico()).orElseGet(PerfilMedico::new);
        perfil.setEspecialidadId(esp.id());
        perfil.setRegistroProfesional(request.registroProfesional().trim());
        usuario.setPerfilMedico(perfil);
        usuario.setNombre(request.nombre().trim());
        return mapper.toResponse(usuarioRepository.save(usuario), Map.of(esp.id(), esp.nombre()));
    }

    @Override
    @Transactional(readOnly = true)
    public long contarActivosPorEspecialidad(Long especialidadId) {
        return usuarioRepository.findMedicosAgendablesPorEspecialidad(especialidadId).size();
    }

    private void exigirAdmin(UsuarioContexto ctx) {
        if (!ctx.tieneRol(UsuarioContexto.ROL_ADMIN)) {
            throw new ForbiddenOperationException("Operación permitida solo para administradores");
        }
    }
}

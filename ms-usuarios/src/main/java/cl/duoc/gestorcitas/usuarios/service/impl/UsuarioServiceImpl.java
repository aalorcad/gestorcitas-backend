package cl.duoc.gestorcitas.usuarios.service.impl;

import cl.duoc.gestorcitas.usuarios.dto.*;
import cl.duoc.gestorcitas.usuarios.entity.PerfilMedico;
import cl.duoc.gestorcitas.usuarios.entity.PerfilPaciente;
import cl.duoc.gestorcitas.usuarios.entity.RolUsuario;
import cl.duoc.gestorcitas.usuarios.entity.Usuario;
import cl.duoc.gestorcitas.usuarios.exception.BusinessException;
import cl.duoc.gestorcitas.usuarios.exception.ForbiddenOperationException;
import cl.duoc.gestorcitas.usuarios.exception.ResourceNotFoundException;
import cl.duoc.gestorcitas.usuarios.mapper.UsuarioMapper;
import cl.duoc.gestorcitas.usuarios.repository.PerfilPacienteRepository;
import cl.duoc.gestorcitas.usuarios.repository.UsuarioRepository;
import cl.duoc.gestorcitas.usuarios.service.UsuarioService;
import cl.duoc.gestorcitas.usuarios.util.RutUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Reglas de negocio de los usuarios (pacientes, médicos y administradores). */
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final int EDAD_MAXIMA = 120;

    private final UsuarioRepository usuarioRepository;
    private final PerfilPacienteRepository perfilPacienteRepository;
    private final UsuarioMapper mapper;
    private final EspecialidadResolver especialidades;
    private final Clock clock;

    // ------------------------------------------------------------------ Usuario autenticado

    @Override
    @Transactional
    public UsuarioResponse sincronizar(UsuarioContexto ctx) {
        String email = normalizarEmail(ctx.email());
        Set<RolUsuario> roles = RolUsuario.desdeAppRoles(ctx.roles());

        Usuario usuario = usuarioRepository.findByOid(ctx.id())
                .orElseGet(() -> vincularOPreparar(ctx, email));

        // Entra ID es la fuente de verdad para identidad y roles
        usuario.setOid(ctx.id());
        if (StringUtils.hasText(ctx.nombre())) usuario.setNombre(ctx.nombre());
        if (email != null) usuario.setEmail(email);
        usuario.getRoles().clear();
        usuario.getRoles().addAll(roles);

        if (usuario.tieneRol(RolUsuario.PACIENTE) && usuario.getPerfilPaciente() == null) {
            usuario.setPerfilPaciente(new PerfilPaciente());
        }
        if (usuario.tieneRol(RolUsuario.MEDICO) && usuario.getPerfilMedico() == null) {
            usuario.setPerfilMedico(new PerfilMedico()); // queda pendiente de especialidad
        }
        usuario.setUltimoAcceso(ahora());

        return responder(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerActual(UsuarioContexto ctx) {
        return responder(buscarPorOid(ctx.id()));
    }

    @Override
    @Transactional
    public UsuarioResponse actualizarPerfilPaciente(ActualizarPerfilPacienteRequest request, UsuarioContexto ctx) {
        Usuario usuario = buscarActivoPorOid(ctx.id());
        if (!usuario.tieneRol(RolUsuario.PACIENTE)) {
            throw new ForbiddenOperationException("Solo un paciente puede editar su perfil de paciente");
        }
        if (!RutUtils.esValido(request.rut())) {
            throw new BusinessException("El RUT ingresado no es válido");
        }
        String rut = RutUtils.normalizar(request.rut());

        PerfilPaciente perfil = Optional.ofNullable(usuario.getPerfilPaciente()).orElseGet(PerfilPaciente::new);
        boolean rutDuplicado = perfil.getId() == null
                ? perfilPacienteRepository.existsByRut(rut)
                : perfilPacienteRepository.existsByRutAndIdNot(rut, perfil.getId());
        if (rutDuplicado) {
            throw new BusinessException("El RUT " + rut + " ya está registrado por otro paciente");
        }
        LocalDate hoy = LocalDate.now(clock);
        if (!request.fechaNacimiento().isBefore(hoy) || request.fechaNacimiento().isBefore(hoy.minusYears(EDAD_MAXIMA))) {
            throw new BusinessException("La fecha de nacimiento no es válida");
        }

        perfil.setRut(rut);
        perfil.setFechaNacimiento(request.fechaNacimiento());
        perfil.setPrevision(request.prevision());
        usuario.setPerfilPaciente(perfil);
        usuario.setTelefono(limpiar(request.telefono()));
        return responder(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse actualizarPerfilMedico(ActualizarPerfilMedicoRequest request, UsuarioContexto ctx) {
        Usuario usuario = buscarActivoPorOid(ctx.id());
        if (!usuario.tieneRol(RolUsuario.MEDICO)) {
            throw new ForbiddenOperationException("Solo un médico puede editar su perfil profesional");
        }
        PerfilMedico perfil = Optional.ofNullable(usuario.getPerfilMedico()).orElseGet(PerfilMedico::new);
        perfil.setBiografia(limpiar(request.biografia()));
        usuario.setPerfilMedico(perfil);
        usuario.setTelefono(limpiar(request.telefono()));
        return responder(usuarioRepository.save(usuario));
    }

    // ------------------------------------------------------------------ Administración

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(String rol, String q, UsuarioContexto ctx) {
        exigirAdmin(ctx);
        RolUsuario r = null;
        if (StringUtils.hasText(rol)) {
            r = RolUsuario.desdeAppRole(rol);
            if (r == null) throw new BusinessException("Rol desconocido: " + rol);
        }
        // Sin texto de búsqueda se usa la consulta sin LIKE (Oracle trata '' como NULL).
        // Con texto, el patrón se arma aquí: "%texto%" en minúsculas.
        boolean conFiltro = StringUtils.hasText(q);
        String filtro = conFiltro ? patronLike(q) : null;
        List<Usuario> usuarios;
        if (r != null) {
            usuarios = conFiltro ? usuarioRepository.buscarPorRol(r, filtro) : usuarioRepository.listarPorRol(r);
        } else {
            usuarios = conFiltro ? usuarioRepository.buscar(filtro) : usuarioRepository.findAllByOrderByNombreAsc();
        }
        Map<Long, String> nombres = especialidades.nombres();
        return usuarios.stream().map(u -> mapper.toResponse(u, nombres)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id, UsuarioContexto ctx) {
        exigirAdmin(ctx);
        return responder(buscar(id));
    }

    @Override
    @Transactional
    public UsuarioResponse activar(Long id, UsuarioContexto ctx) {
        exigirAdmin(ctx);
        Usuario usuario = buscar(id);
        usuario.setActivo(true);
        return responder(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse desactivar(Long id, UsuarioContexto ctx) {
        exigirAdmin(ctx);
        Usuario usuario = buscar(id);
        if (ctx.id().equals(usuario.getOid())) {
            throw new BusinessException("No puedes desactivar tu propia cuenta");
        }
        if (usuario.tieneRol(RolUsuario.ADMIN) && usuario.isActivo()
                && usuarioRepository.contarActivosPorRol(RolUsuario.ADMIN) <= 1) {
            throw new BusinessException("Debe existir al menos un administrador activo");
        }
        usuario.setActivo(false);
        return responder(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenUsuariosResponse resumen(UsuarioContexto ctx) {
        exigirAdmin(ctx);
        return new ResumenUsuariosResponse(
                usuarioRepository.count(),
                usuarioRepository.contarPorRol(RolUsuario.PACIENTE),
                usuarioRepository.contarPorRol(RolUsuario.MEDICO),
                usuarioRepository.contarPorRol(RolUsuario.ADMIN),
                usuarioRepository.contarActivosPorRol(RolUsuario.MEDICO),
                usuarioRepository.countByActivoFalse()
        );
    }

    // ------------------------------------------------------------------ ms-citas

    @Override
    @Transactional(readOnly = true)
    public PacienteEstadoResponse estadoPaciente(String oid) {
        return usuarioRepository.findByOid(oid)
                .map(u -> new PacienteEstadoResponse(
                        oid,
                        true,
                        u.isActivo(),
                        u.getPerfilPaciente() != null && u.getPerfilPaciente().isCompleto(),
                        u.getPerfilPaciente() == null ? null : u.getPerfilPaciente().getRut()))
                .orElse(new PacienteEstadoResponse(oid, false, false, false, null));
    }

    // ------------------------------------------------------------------ privados

    /** Primer login: si un Admin pre-registró el email (médico), se vincula; si no, se crea. */
    private Usuario vincularOPreparar(UsuarioContexto ctx, String email) {
        if (email != null) {
            Optional<Usuario> existente = usuarioRepository.findByEmailIgnoreCase(email);
            if (existente.isPresent()) {
                Usuario u = existente.get();
                if (u.getOid() != null && !u.getOid().equals(ctx.id())) {
                    throw new BusinessException("El email " + email + " ya está vinculado a otra cuenta");
                }
                return u;
            }
        }
        if (email == null) {
            throw new BusinessException("El token no contiene email/UPN; no es posible registrar al usuario");
        }
        return Usuario.builder()
                .email(email)
                .nombre(StringUtils.hasText(ctx.nombre()) ? ctx.nombre() : email)
                .activo(true)
                .creadoEn(ahora())
                .build();
    }

    private UsuarioResponse responder(Usuario u) {
        Map<Long, String> nombres = u.getPerfilMedico() != null && u.getPerfilMedico().getEspecialidadId() != null
                ? especialidades.nombres()
                : Map.of();
        return mapper.toResponse(u, nombres);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + id + " no encontrado"));
    }

    private Usuario buscarPorOid(String oid) {
        return usuarioRepository.findByOid(oid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no registrado. Debe sincronizarse primero."));
    }

    private Usuario buscarActivoPorOid(String oid) {
        Usuario u = buscarPorOid(oid);
        if (!u.isActivo()) {
            throw new ForbiddenOperationException("Tu cuenta está desactivada");
        }
        return u;
    }

    private void exigirAdmin(UsuarioContexto ctx) {
        if (!ctx.tieneRol(UsuarioContexto.ROL_ADMIN)) {
            throw new ForbiddenOperationException("Operación permitida solo para administradores");
        }
    }

    /** "50%_x" -> "%50!%!_x%": escapa los comodines que escriba el usuario (escape '!'). */
    private String patronLike(String q) {
        String texto = q.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + texto + "%";
    }

    private String normalizarEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String limpiar(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}

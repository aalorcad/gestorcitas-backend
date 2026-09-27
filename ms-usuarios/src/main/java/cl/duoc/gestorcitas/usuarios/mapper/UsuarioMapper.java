package cl.duoc.gestorcitas.usuarios.mapper;

import cl.duoc.gestorcitas.usuarios.dto.*;
import cl.duoc.gestorcitas.usuarios.entity.PerfilMedico;
import cl.duoc.gestorcitas.usuarios.entity.PerfilPaciente;
import cl.duoc.gestorcitas.usuarios.entity.RolUsuario;
import cl.duoc.gestorcitas.usuarios.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;

@Component
public class UsuarioMapper {

    /**
     * @param nombresEspecialidad mapa id -> nombre para resolver la especialidad del médico
     */
    public UsuarioResponse toResponse(Usuario u, Map<Long, String> nombresEspecialidad) {
        return new UsuarioResponse(
                u.getId(),
                u.getOid(),
                u.getEmail(),
                u.getNombre(),
                u.getTelefono(),
                u.isActivo(),
                u.getOid() != null,
                u.getRoles().stream().sorted(Comparator.naturalOrder()).map(RolUsuario::getAppRole).toList(),
                toDto(u.getPerfilPaciente()),
                toDto(u.getPerfilMedico(), nombresEspecialidad),
                u.getCreadoEn(),
                u.getUltimoAcceso()
        );
    }

    public MedicoResponse toMedicoResponse(Usuario u, String especialidadNombre) {
        PerfilMedico p = u.getPerfilMedico();
        Long especialidadId = p == null ? null : p.getEspecialidadId();
        return new MedicoResponse(
                u.getId(),
                u.getNombre(),
                u.getEmail(),
                u.getTelefono(),
                especialidadId,
                especialidadNombre,
                p == null ? null : p.getRegistroProfesional(),
                p == null ? null : p.getBiografia(),
                u.isActivo(),
                u.isActivo() && especialidadId != null
        );
    }

    private PerfilPacienteDto toDto(PerfilPaciente p) {
        if (p == null) return null;
        return new PerfilPacienteDto(p.getRut(), p.getFechaNacimiento(), p.getPrevision(), p.isCompleto());
    }

    private PerfilMedicoDto toDto(PerfilMedico p, Map<Long, String> nombres) {
        if (p == null) return null;
        String nombre = p.getEspecialidadId() == null ? null : nombres.get(p.getEspecialidadId());
        return new PerfilMedicoDto(p.getEspecialidadId(), nombre, p.getRegistroProfesional(),
                p.getBiografia(), p.getEspecialidadId() != null);
    }
}

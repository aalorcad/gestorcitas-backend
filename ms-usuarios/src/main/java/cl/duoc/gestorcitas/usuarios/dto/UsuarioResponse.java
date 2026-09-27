package cl.duoc.gestorcitas.usuarios.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UsuarioResponse(
        Long id,
        String oid,
        String email,
        String nombre,
        String telefono,
        boolean activo,
        boolean vinculadoEntraId,
        List<String> roles,
        PerfilPacienteDto perfilPaciente,
        PerfilMedicoDto perfilMedico,
        LocalDateTime creadoEn,
        LocalDateTime ultimoAcceso
) {
}

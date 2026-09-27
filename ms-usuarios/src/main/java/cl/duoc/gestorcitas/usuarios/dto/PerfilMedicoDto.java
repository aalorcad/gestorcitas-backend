package cl.duoc.gestorcitas.usuarios.dto;

public record PerfilMedicoDto(Long especialidadId, String especialidadNombre,
                              String registroProfesional, String biografia, boolean especialidadAsignada) {
}

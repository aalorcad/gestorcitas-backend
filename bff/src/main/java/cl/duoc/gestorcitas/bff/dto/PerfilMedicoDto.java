package cl.duoc.gestorcitas.bff.dto;

public record PerfilMedicoDto(Long especialidadId, String especialidadNombre, String registroProfesional,
                              String biografia, boolean especialidadAsignada) {
}

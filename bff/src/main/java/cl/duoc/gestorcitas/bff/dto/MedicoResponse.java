package cl.duoc.gestorcitas.bff.dto;

public record MedicoResponse(Long id, String nombre, String email, String telefono, Long especialidadId,
                             String especialidadNombre, String registroProfesional, String biografia,
                             boolean activo, boolean disponible) {
}

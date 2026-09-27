package cl.duoc.gestorcitas.usuarios.dto;

/** Vista pública de un médico (reserva de citas y validación desde ms-citas). */
public record MedicoResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        Long especialidadId,
        String especialidadNombre,
        String registroProfesional,
        String biografia,
        boolean activo,
        boolean disponible
) {
}

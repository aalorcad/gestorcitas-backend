package cl.duoc.gestorcitas.citas.dto;

/** Médico devuelto por ms-usuarios. "disponible" = activo y con especialidad asignada. */
public record MedicoDto(
        Long id,
        String nombre,
        String email,
        Long especialidadId,
        String especialidadNombre,
        boolean activo,
        boolean disponible
) {
}

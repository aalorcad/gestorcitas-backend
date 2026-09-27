package cl.duoc.gestorcitas.citas.dto;

/** Estado del paciente devuelto por ms-usuarios. */
public record PacienteEstadoDto(String oid, boolean registrado, boolean activo, boolean perfilCompleto, String rut) {
}

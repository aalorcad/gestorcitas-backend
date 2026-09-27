package cl.duoc.gestorcitas.usuarios.dto;

/** Estado mínimo que ms-citas necesita para permitir una reserva. */
public record PacienteEstadoResponse(String oid, boolean registrado, boolean activo, boolean perfilCompleto, String rut) {
}

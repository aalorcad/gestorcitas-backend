package cl.duoc.gestorcitas.bff.dto;

public record ResumenUsuariosResponse(long totalUsuarios, long pacientes, long medicos, long administradores,
                                      long medicosActivos, long inactivos) {
}

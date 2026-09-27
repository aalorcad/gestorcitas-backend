package cl.duoc.gestorcitas.bff.dto;

/** Agregado de indicadores para el dashboard del Admin (compone 3 microservicios). */
public record DashboardAdminResponse(
        ResumenUsuariosResponse usuarios,
        ResumenCitasResponse citas,
        long especialidadesActivas
) {
}

package cl.duoc.gestorcitas.citas.dto;

public record ResumenCitasResponse(
        long total,
        long pendientes,
        long confirmadas,
        long atendidas,
        long canceladas,
        long hoy
) {
}

package cl.duoc.gestorcitas.bff.dto;

public record ResumenCitasResponse(long total, long pendientes, long confirmadas, long atendidas,
                                   long canceladas, long hoy) {
}

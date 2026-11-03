package cl.duoc.gestorcitas.catalogo.dto;

public record EspecialidadResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer valorConsulta,
        boolean activa
) {
}

package cl.duoc.gestorcitas.catalogo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EspecialidadRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String descripcion,

        @NotNull(message = "El valor de la consulta es obligatorio")
        @Min(value = 0, message = "El valor no puede ser negativo")
        @Max(value = 1_000_000, message = "El valor no puede superar $1.000.000")
        Integer valorConsulta
) {
}

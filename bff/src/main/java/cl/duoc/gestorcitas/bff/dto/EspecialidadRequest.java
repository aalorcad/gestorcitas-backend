package cl.duoc.gestorcitas.bff.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EspecialidadRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        @Size(max = 255) String descripcion,
        @NotNull(message = "El valor de la consulta es obligatorio") @Min(0) @Max(1_000_000) Integer valorConsulta
) {
}

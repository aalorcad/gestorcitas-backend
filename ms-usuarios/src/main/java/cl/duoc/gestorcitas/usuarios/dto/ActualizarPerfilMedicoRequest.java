package cl.duoc.gestorcitas.usuarios.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que el propio médico puede editar. */
public record ActualizarPerfilMedicoRequest(
        @Pattern(regexp = "^$|^\\+?56\\s?9\\s?\\d{4}\\s?\\d{4}$", message = "Teléfono inválido (ej: +56 9 1234 5678)")
        String telefono,

        @Size(max = 500, message = "La biografía no puede superar 500 caracteres")
        String biografia
) {
}

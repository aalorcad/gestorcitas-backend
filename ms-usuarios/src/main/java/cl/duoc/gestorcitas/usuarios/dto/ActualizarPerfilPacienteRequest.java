package cl.duoc.gestorcitas.usuarios.dto;

import cl.duoc.gestorcitas.usuarios.entity.Prevision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record ActualizarPerfilPacienteRequest(
        @NotBlank(message = "El RUT es obligatorio")
        String rut,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser pasada")
        LocalDate fechaNacimiento,

        @NotNull(message = "La previsión es obligatoria")
        Prevision prevision,

        @Pattern(regexp = "^$|^\\+?56\\s?9\\s?\\d{4}\\s?\\d{4}$", message = "Teléfono inválido (ej: +56 9 1234 5678)")
        String telefono
) {
}

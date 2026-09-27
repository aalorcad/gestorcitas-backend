package cl.duoc.gestorcitas.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record ActualizarPerfilPacienteRequest(
        @NotBlank(message = "El RUT es obligatorio") String rut,
        @NotNull(message = "La fecha de nacimiento es obligatoria") @Past LocalDate fechaNacimiento,
        @NotBlank(message = "La previsión es obligatoria") String prevision,
        String telefono
) {
}

package cl.duoc.gestorcitas.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Pre-registro de un médico por el Admin (se vincula a Entra ID en su primer login). */
public record RegistrarMedicoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email inválido") @Size(max = 150) String email,
        @Pattern(regexp = "^$|^\\+?56\\s?9\\s?\\d{4}\\s?\\d{4}$", message = "Teléfono inválido") String telefono,
        @NotNull(message = "La especialidad es obligatoria") Long especialidadId,
        @NotBlank(message = "El registro profesional es obligatorio") @Size(max = 30) String registroProfesional
) {
}

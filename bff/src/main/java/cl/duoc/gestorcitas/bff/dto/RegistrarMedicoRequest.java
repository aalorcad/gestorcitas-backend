package cl.duoc.gestorcitas.bff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarMedicoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @NotBlank(message = "El email es obligatorio") @Email @Size(max = 150) String email,
        String telefono,
        @NotNull(message = "La especialidad es obligatoria") Long especialidadId,
        @NotBlank(message = "El registro profesional es obligatorio") @Size(max = 30) String registroProfesional
) {
}

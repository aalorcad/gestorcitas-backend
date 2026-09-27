package cl.duoc.gestorcitas.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos profesionales del médico que administra el Admin. */
public record AsignarPerfilMedicoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @NotNull(message = "La especialidad es obligatoria") Long especialidadId,
        @NotBlank(message = "El registro profesional es obligatorio") @Size(max = 30) String registroProfesional
) {
}

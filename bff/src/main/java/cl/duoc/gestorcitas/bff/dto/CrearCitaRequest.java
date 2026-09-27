package cl.duoc.gestorcitas.bff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CrearCitaRequest(
        @NotNull(message = "El médico es obligatorio") Long medicoId,
        @NotNull(message = "La fecha y hora es obligatoria") LocalDateTime fechaHora,
        @Size(max = 255) String motivo
) {
}

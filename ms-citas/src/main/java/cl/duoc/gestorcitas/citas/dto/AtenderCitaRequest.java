package cl.duoc.gestorcitas.citas.dto;

import jakarta.validation.constraints.Size;

public record AtenderCitaRequest(
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones
) {
}

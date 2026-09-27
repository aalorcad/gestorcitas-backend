package cl.duoc.gestorcitas.bff.dto;

import jakarta.validation.constraints.Size;

public record ActualizarPerfilMedicoRequest(String telefono, @Size(max = 500) String biografia) {
}

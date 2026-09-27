package cl.duoc.gestorcitas.bff.dto;

import java.time.LocalDate;

public record PerfilPacienteDto(String rut, LocalDate fechaNacimiento, String prevision, boolean completo) {
}

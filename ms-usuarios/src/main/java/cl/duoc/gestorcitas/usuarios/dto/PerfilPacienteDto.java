package cl.duoc.gestorcitas.usuarios.dto;

import cl.duoc.gestorcitas.usuarios.entity.Prevision;

import java.time.LocalDate;

public record PerfilPacienteDto(String rut, LocalDate fechaNacimiento, Prevision prevision, boolean completo) {
}

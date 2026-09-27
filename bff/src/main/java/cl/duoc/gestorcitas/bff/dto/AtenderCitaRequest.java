package cl.duoc.gestorcitas.bff.dto;

import jakarta.validation.constraints.Size;

public record AtenderCitaRequest(@Size(max = 500) String observaciones) {
}

package cl.duoc.gestorcitas.bff.service;

import cl.duoc.gestorcitas.bff.dto.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;

public interface CitaService {

    CitaResponse reservar(CrearCitaRequest request, Jwt jwt);

    List<CitaResponse> misCitas(Jwt jwt);

    CitaResponse cancelar(Long id, Jwt jwt);

    List<CitaResponse> agenda(LocalDate fecha, Jwt jwt);

    CitaResponse confirmar(Long id, Jwt jwt);

    CitaResponse atender(Long id, AtenderCitaRequest request, Jwt jwt);

    List<CitaResponse> todas(String estado, Jwt jwt);

    DisponibilidadResponse disponibilidad(Long medicoId, LocalDate fecha, Jwt jwt);
}

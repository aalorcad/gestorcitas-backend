package cl.duoc.gestorcitas.bff.service.impl;

import cl.duoc.gestorcitas.bff.client.CitasClient;
import cl.duoc.gestorcitas.bff.dto.*;
import cl.duoc.gestorcitas.bff.service.CitaService;
import cl.duoc.gestorcitas.bff.service.IdentidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Orquesta las operaciones de citas: resuelve la identidad desde el JWT validado
 * y la propaga al microservicio de citas.
 */
@Service
@RequiredArgsConstructor
public class CitaServiceImpl implements CitaService {

    private final CitasClient citasClient;
    private final IdentidadService identidadService;

    @Override
    public CitaResponse reservar(CrearCitaRequest request, Jwt jwt) {
        return citasClient.reservar(request, identidadService.desdeToken(jwt));
    }

    @Override
    public List<CitaResponse> misCitas(Jwt jwt) {
        return citasClient.misCitas(identidadService.desdeToken(jwt));
    }

    @Override
    public CitaResponse cancelar(Long id, Jwt jwt) {
        return citasClient.cancelar(id, identidadService.desdeToken(jwt));
    }

    @Override
    public List<CitaResponse> agenda(LocalDate fecha, Jwt jwt) {
        return citasClient.agenda(fecha, identidadService.desdeToken(jwt));
    }

    @Override
    public CitaResponse confirmar(Long id, Jwt jwt) {
        return citasClient.confirmar(id, identidadService.desdeToken(jwt));
    }

    @Override
    public CitaResponse atender(Long id, AtenderCitaRequest request, Jwt jwt) {
        return citasClient.atender(id, request, identidadService.desdeToken(jwt));
    }

    @Override
    public List<CitaResponse> todas(String estado, Jwt jwt) {
        return citasClient.todas(estado, identidadService.desdeToken(jwt));
    }

    @Override
    public DisponibilidadResponse disponibilidad(Long medicoId, LocalDate fecha, Jwt jwt) {
        return citasClient.disponibilidad(medicoId, fecha, identidadService.desdeToken(jwt));
    }
}

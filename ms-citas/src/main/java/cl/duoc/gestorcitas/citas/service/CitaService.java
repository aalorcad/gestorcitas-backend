package cl.duoc.gestorcitas.citas.service;

import cl.duoc.gestorcitas.citas.dto.*;
import cl.duoc.gestorcitas.citas.entity.EstadoCita;

import java.time.LocalDate;
import java.util.List;

public interface CitaService {

    /** Paciente reserva una cita. */
    CitaResponse reservar(CrearCitaRequest request, UsuarioContexto usuario);

    /** Citas del paciente autenticado. */
    List<CitaResponse> listarMisCitas(UsuarioContexto usuario);

    /** Paciente (dueño) o Admin cancelan una cita activa y futura. */
    CitaResponse cancelar(Long citaId, UsuarioContexto usuario);

    /** Agenda del médico autenticado (opcionalmente filtrada por fecha). */
    List<CitaResponse> listarAgenda(LocalDate fecha, UsuarioContexto usuario);

    /** Médico confirma una cita pendiente propia. */
    CitaResponse confirmar(Long citaId, UsuarioContexto usuario);

    /** Médico registra la atención de una cita propia. */
    CitaResponse atender(Long citaId, AtenderCitaRequest request, UsuarioContexto usuario);

    /** Admin: todas las citas, opcionalmente por estado. */
    List<CitaResponse> listarTodas(EstadoCita estado, UsuarioContexto usuario);

    /** Admin: indicadores de citas por estado. */
    ResumenCitasResponse resumen(UsuarioContexto usuario);

    /** Horarios libres de un médico para una fecha. */
    DisponibilidadResponse disponibilidad(Long medicoId, LocalDate fecha);
}

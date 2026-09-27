package cl.duoc.gestorcitas.citas.service.impl;

import cl.duoc.gestorcitas.citas.client.UsuariosClient;
import cl.duoc.gestorcitas.citas.config.AgendaProperties;
import cl.duoc.gestorcitas.citas.dto.*;
import cl.duoc.gestorcitas.citas.entity.Cita;
import cl.duoc.gestorcitas.citas.entity.EstadoCita;
import cl.duoc.gestorcitas.citas.exception.BusinessException;
import cl.duoc.gestorcitas.citas.exception.ForbiddenOperationException;
import cl.duoc.gestorcitas.citas.exception.ResourceNotFoundException;
import cl.duoc.gestorcitas.citas.mapper.CitaMapper;
import cl.duoc.gestorcitas.citas.repository.CitaRepository;
import cl.duoc.gestorcitas.citas.service.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contiene TODAS las reglas de negocio de las citas médicas.
 */
@Service
@RequiredArgsConstructor
public class CitaServiceImpl implements CitaService {

    private static final Set<EstadoCita> ESTADOS_ACTIVOS = EnumSet.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

    private final CitaRepository citaRepository;
    private final UsuariosClient usuariosClient;
    private final CitaMapper mapper;
    private final AgendaProperties agenda;
    private final Clock clock;

    // ------------------------------------------------------------------ Paciente

    @Override
    @Transactional
    public CitaResponse reservar(CrearCitaRequest request, UsuarioContexto usuario) {
        exigirRol(usuario, UsuarioContexto.ROL_PACIENTE);
        LocalDateTime fechaHora = request.fechaHora().truncatedTo(ChronoUnit.MINUTES);

        validarHorarioDeAtencion(fechaHora);

        PacienteEstadoDto paciente = usuariosClient.estadoPaciente(usuario.id());
        if (!paciente.registrado() || !paciente.activo()) {
            throw new ForbiddenOperationException("Tu cuenta de paciente no está habilitada para reservar");
        }
        if (!paciente.perfilCompleto()) {
            throw new BusinessException("Completa tu perfil (RUT, fecha de nacimiento y previsión) antes de reservar");
        }

        MedicoDto medico = usuariosClient.obtenerMedico(request.medicoId());
        if (!medico.disponible()) {
            throw new BusinessException("El médico seleccionado no está disponible");
        }
        if (citaRepository.existsByMedicoIdAndFechaHoraAndEstadoIn(medico.id(), fechaHora, ESTADOS_ACTIVOS)) {
            throw new BusinessException("El horario seleccionado ya está reservado para este médico");
        }
        if (citaRepository.existsByPacienteIdAndFechaHoraAndEstadoIn(usuario.id(), fechaHora, ESTADOS_ACTIVOS)) {
            throw new BusinessException("Ya tienes otra cita reservada en ese mismo horario");
        }
        long activasEnEspecialidad = citaRepository.countByPacienteIdAndEspecialidadIdAndFechaHoraAfterAndEstadoIn(
                usuario.id(), medico.especialidadId(), ahora(), ESTADOS_ACTIVOS);
        if (activasEnEspecialidad >= agenda.maxCitasActivasPorEspecialidad()) {
            throw new BusinessException("Ya tienes una cita activa en " + medico.especialidadNombre()
                    + ". Cancélala o espera a ser atendido antes de reservar otra.");
        }

        LocalDateTime ahora = ahora();
        Cita cita = Cita.builder()
                .pacienteId(usuario.id())
                .pacienteNombre(usuario.nombre())
                .pacienteEmail(usuario.email())
                .medicoId(medico.id())
                .medicoNombre(medico.nombre())
                .medicoEmail(medico.email())
                .especialidadId(medico.especialidadId())
                .especialidadNombre(medico.especialidadNombre())
                .fechaHora(fechaHora)
                .motivo(request.motivo())
                .estado(EstadoCita.PENDIENTE)
                .creadaEn(ahora)
                .actualizadaEn(ahora)
                .build();
        return mapper.toResponse(citaRepository.save(cita));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listarMisCitas(UsuarioContexto usuario) {
        exigirRol(usuario, UsuarioContexto.ROL_PACIENTE);
        return citaRepository.findByPacienteIdOrderByFechaHoraDesc(usuario.id())
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public CitaResponse cancelar(Long citaId, UsuarioContexto usuario) {
        Cita cita = buscar(citaId);
        boolean esDueno = cita.getPacienteId().equals(usuario.id());
        if (!esDueno && !usuario.tieneRol(UsuarioContexto.ROL_ADMIN)) {
            throw new ForbiddenOperationException("Solo el paciente dueño o un administrador pueden cancelar esta cita");
        }
        if (!cita.getEstado().esActiva()) {
            throw new BusinessException("Solo se pueden cancelar citas pendientes o confirmadas (estado actual: "
                    + cita.getEstado() + ")");
        }
        if (!cita.getFechaHora().isAfter(ahora())) {
            throw new BusinessException("No se puede cancelar una cita cuya hora ya pasó");
        }
        return cambiarEstado(cita, EstadoCita.CANCELADA);
    }

    // ------------------------------------------------------------------ Médico

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listarAgenda(LocalDate fecha, UsuarioContexto usuario) {
        exigirRol(usuario, UsuarioContexto.ROL_MEDICO);
        List<Cita> citas = (fecha == null)
                ? citaRepository.findByMedicoEmailIgnoreCaseOrderByFechaHoraAsc(usuario.email())
                : citaRepository.findByMedicoEmailIgnoreCaseAndFechaHoraBetweenOrderByFechaHoraAsc(
                        usuario.email(), fecha.atStartOfDay(), fecha.atTime(LocalTime.MAX));
        return citas.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public CitaResponse confirmar(Long citaId, UsuarioContexto usuario) {
        Cita cita = buscarCitaDelMedico(citaId, usuario);
        if (cita.getEstado() != EstadoCita.PENDIENTE) {
            throw new BusinessException("Solo se pueden confirmar citas pendientes");
        }
        return cambiarEstado(cita, EstadoCita.CONFIRMADA);
    }

    @Override
    @Transactional
    public CitaResponse atender(Long citaId, AtenderCitaRequest request, UsuarioContexto usuario) {
        Cita cita = buscarCitaDelMedico(citaId, usuario);
        if (!cita.getEstado().esActiva()) {
            throw new BusinessException("Solo se pueden atender citas pendientes o confirmadas");
        }
        cita.setObservaciones(request == null ? null : request.observaciones());
        return cambiarEstado(cita, EstadoCita.ATENDIDA);
    }

    // ------------------------------------------------------------------ Admin / consulta

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listarTodas(EstadoCita estado, UsuarioContexto usuario) {
        exigirRol(usuario, UsuarioContexto.ROL_ADMIN);
        List<Cita> citas = (estado == null)
                ? citaRepository.findAllByOrderByFechaHoraDesc()
                : citaRepository.findByEstadoOrderByFechaHoraDesc(estado);
        return citas.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadResponse disponibilidad(Long medicoId, LocalDate fecha) {
        MedicoDto medico = usuariosClient.obtenerMedico(medicoId);
        if (!medico.disponible() || !agenda.diasHabiles().contains(fecha.getDayOfWeek()) || fecha.isBefore(hoy())) {
            return new DisponibilidadResponse(medicoId, medico.nombre(), fecha, List.of());
        }

        Set<LocalDateTime> ocupados = citaRepository.findByMedicoIdAndFechaHoraBetweenAndEstadoIn(
                        medicoId, fecha.atStartOfDay(), fecha.atTime(LocalTime.MAX), ESTADOS_ACTIVOS)
                .stream().map(Cita::getFechaHora).collect(Collectors.toSet());

        LocalDateTime ahora = ahora();
        List<LocalDateTime> libres = new ArrayList<>();
        for (LocalTime t = agenda.inicio(); t.isBefore(agenda.fin()); t = t.plusMinutes(agenda.duracionMinutos())) {
            LocalDateTime slot = fecha.atTime(t);
            if (slot.isAfter(ahora) && !ocupados.contains(slot)) {
                libres.add(slot);
            }
        }
        return new DisponibilidadResponse(medicoId, medico.nombre(), fecha, libres);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenCitasResponse resumen(UsuarioContexto usuario) {
        exigirRol(usuario, UsuarioContexto.ROL_ADMIN);
        LocalDate hoy = hoy();
        return new ResumenCitasResponse(
                citaRepository.count(),
                citaRepository.countByEstado(EstadoCita.PENDIENTE),
                citaRepository.countByEstado(EstadoCita.CONFIRMADA),
                citaRepository.countByEstado(EstadoCita.ATENDIDA),
                citaRepository.countByEstado(EstadoCita.CANCELADA),
                citaRepository.countByFechaHoraBetween(hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX)));
    }

    // ------------------------------------------------------------------ Reglas privadas

    private void validarHorarioDeAtencion(LocalDateTime fechaHora) {
        if (!fechaHora.isAfter(ahora())) {
            throw new BusinessException("La cita debe agendarse para una fecha y hora futura");
        }
        if (!agenda.diasHabiles().contains(fechaHora.getDayOfWeek())) {
            throw new BusinessException("Solo se atiende en días hábiles");
        }
        LocalTime hora = fechaHora.toLocalTime();
        LocalTime termino = hora.plusMinutes(agenda.duracionMinutos());
        if (hora.isBefore(agenda.inicio()) || termino.isAfter(agenda.fin()) || termino.isBefore(hora)) {
            throw new BusinessException("El horario de atención es de " + agenda.inicio() + " a " + agenda.fin());
        }
        long minutosDesdeInicio = Duration.between(agenda.inicio(), hora).toMinutes();
        if (minutosDesdeInicio % agenda.duracionMinutos() != 0) {
            throw new BusinessException("Las citas se agendan en bloques de " + agenda.duracionMinutos() + " minutos");
        }
    }

    private Cita buscarCitaDelMedico(Long citaId, UsuarioContexto usuario) {
        exigirRol(usuario, UsuarioContexto.ROL_MEDICO);
        Cita cita = buscar(citaId);
        if (usuario.email() == null || !cita.getMedicoEmail().equalsIgnoreCase(usuario.email())) {
            throw new ForbiddenOperationException("Esta cita no pertenece a tu agenda");
        }
        return cita;
    }

    private CitaResponse cambiarEstado(Cita cita, EstadoCita nuevoEstado) {
        cita.setEstado(nuevoEstado);
        cita.setActualizadaEn(ahora());
        return mapper.toResponse(citaRepository.save(cita));
    }

    private Cita buscar(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita " + id + " no encontrada"));
    }

    private void exigirRol(UsuarioContexto usuario, String rol) {
        if (!usuario.tieneRol(rol)) {
            throw new ForbiddenOperationException("Operación permitida solo para el rol " + rol);
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }
}

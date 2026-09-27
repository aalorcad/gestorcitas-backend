package cl.duoc.gestorcitas.citas.service;

import cl.duoc.gestorcitas.citas.client.UsuariosClient;
import cl.duoc.gestorcitas.citas.config.AgendaProperties;
import cl.duoc.gestorcitas.citas.dto.CitaResponse;
import cl.duoc.gestorcitas.citas.dto.CrearCitaRequest;
import cl.duoc.gestorcitas.citas.dto.MedicoDto;
import cl.duoc.gestorcitas.citas.dto.PacienteEstadoDto;
import cl.duoc.gestorcitas.citas.dto.UsuarioContexto;
import cl.duoc.gestorcitas.citas.entity.Cita;
import cl.duoc.gestorcitas.citas.entity.EstadoCita;
import cl.duoc.gestorcitas.citas.exception.BusinessException;
import cl.duoc.gestorcitas.citas.exception.ForbiddenOperationException;
import cl.duoc.gestorcitas.citas.mapper.CitaMapper;
import cl.duoc.gestorcitas.citas.repository.CitaRepository;
import cl.duoc.gestorcitas.citas.service.impl.CitaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    /** Lunes 28-09-2026 09:00 hora de Chile. */
    private static final Clock CLOCK = Clock.fixed(
            LocalDateTime.of(2026, 9, 28, 9, 0).atZone(ZONA).toInstant(), ZONA);

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private UsuariosClient usuariosClient;

    private CitaServiceImpl service;

    private final UsuarioContexto paciente =
            new UsuarioContexto("oid-paciente", "Ana Paciente", "ana@demo.cl", Set.of("Paciente"));
    private final MedicoDto medico =
            new MedicoDto(1L, "Dra. Rojas", "rojas@demo.cl", 10L, "Medicina General", true, true);
    private final PacienteEstadoDto pacienteOk =
            new PacienteEstadoDto("oid-paciente", true, true, true, "11111111-1");

    @BeforeEach
    void setUp() {
        AgendaProperties agenda = new AgendaProperties(null, null, 30, null, 1);
        service = new CitaServiceImpl(citaRepository, usuariosClient, new CitaMapper(), agenda, CLOCK);
    }

    @Test
    void reservaExitosaQuedaPendiente() {
        LocalDateTime martes10 = LocalDateTime.of(2026, 9, 29, 10, 0);
        when(usuariosClient.estadoPaciente("oid-paciente")).thenReturn(pacienteOk);
        when(usuariosClient.obtenerMedico(1L)).thenReturn(medico);
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        CitaResponse r = service.reservar(new CrearCitaRequest(1L, martes10, "Control"), paciente);

        assertThat(r.estado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(r.medicoNombre()).isEqualTo("Dra. Rojas");
        assertThat(r.pacienteId()).isEqualTo("oid-paciente");
    }

    @Test
    void noSePuedeReservarEnFinDeSemana() {
        LocalDateTime sabado = LocalDateTime.of(2026, 10, 3, 10, 0);
        assertThatThrownBy(() -> service.reservar(new CrearCitaRequest(1L, sabado, null), paciente))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("días hábiles");
        verifyNoInteractions(usuariosClient);
    }

    @Test
    void noSePuedeReservarFueraDeBloque() {
        LocalDateTime martes1015 = LocalDateTime.of(2026, 9, 29, 10, 15);
        assertThatThrownBy(() -> service.reservar(new CrearCitaRequest(1L, martes1015, null), paciente))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bloques");
    }

    @Test
    void noSePuedeReservarHorarioOcupado() {
        LocalDateTime martes10 = LocalDateTime.of(2026, 9, 29, 10, 0);
        when(usuariosClient.estadoPaciente("oid-paciente")).thenReturn(pacienteOk);
        when(usuariosClient.obtenerMedico(1L)).thenReturn(medico);
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstadoIn(eq(1L), eq(martes10), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.reservar(new CrearCitaRequest(1L, martes10, null), paciente))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está reservado");
        verify(citaRepository, never()).save(any());
    }

    @Test
    void pacienteConPerfilIncompletoNoPuedeReservar() {
        when(usuariosClient.estadoPaciente("oid-paciente"))
                .thenReturn(new PacienteEstadoDto("oid-paciente", true, true, false, null));

        assertThatThrownBy(() -> service.reservar(
                new CrearCitaRequest(1L, LocalDateTime.of(2026, 9, 29, 10, 0), null), paciente))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Completa tu perfil");
        verify(usuariosClient, never()).obtenerMedico(any());
    }

    @Test
    void medicoNoPuedeReservar() {
        UsuarioContexto medicoUser = new UsuarioContexto("oid-m", "Doc", "rojas@demo.cl", Set.of("Medico"));
        assertThatThrownBy(() -> service.reservar(
                new CrearCitaRequest(1L, LocalDateTime.of(2026, 9, 29, 10, 0), null), medicoUser))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void otroPacienteNoPuedeCancelar() {
        Cita cita = Cita.builder().id(5L).pacienteId("otro-oid").estado(EstadoCita.PENDIENTE)
                .fechaHora(LocalDateTime.of(2026, 9, 29, 10, 0)).build();
        when(citaRepository.findById(5L)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> service.cancelar(5L, paciente))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}

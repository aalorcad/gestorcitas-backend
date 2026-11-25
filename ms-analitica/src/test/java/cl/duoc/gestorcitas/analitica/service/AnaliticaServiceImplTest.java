package cl.duoc.gestorcitas.analitica.service;

import cl.duoc.gestorcitas.analitica.dto.ResumenAnaliticaResponse;
import cl.duoc.gestorcitas.analitica.entity.EventoCita;
import cl.duoc.gestorcitas.analitica.entity.UsuarioEstado;
import cl.duoc.gestorcitas.analitica.messaging.event.CitaEventoMsg;
import cl.duoc.gestorcitas.analitica.messaging.event.UsuarioEstadoMsg;
import cl.duoc.gestorcitas.analitica.repository.ConteoPorClave;
import cl.duoc.gestorcitas.analitica.repository.EventoCitaRepository;
import cl.duoc.gestorcitas.analitica.repository.UsuarioEstadoRepository;
import cl.duoc.gestorcitas.analitica.service.impl.AnaliticaServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AnaliticaServiceImplTest {

    private final EventoCitaRepository eventos = mock(EventoCitaRepository.class);
    private final UsuarioEstadoRepository usuarios = mock(UsuarioEstadoRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneId.of("America/Santiago"));
    private final AnaliticaService servicio = new AnaliticaServiceImpl(eventos, usuarios, clock);

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 9, 28, 8, 0);

    private CitaEventoMsg evento(String id) {
        return new CitaEventoMsg(id, "CREADA", 1L, "oid", 3L, "Dra. Rojas", 10L, "Medicina General",
                T0.plusDays(1), "PENDIENTE", T0);
    }

    private UsuarioEstadoMsg estado(String id, boolean activo, LocalDateTime cuando) {
        return new UsuarioEstadoMsg(id, activo ? "ACTIVADO" : "DESACTIVADO", 7L, "ana@demo.cl", "Ana",
                List.of("Paciente"), activo, cuando);
    }

    @Test
    void eventoNuevoSeGuardaConParticionYOffset() {
        when(eventos.existsByEventoId("e1")).thenReturn(false);

        assertThat(servicio.registrarEventoCita(evento("e1"), 4, 120L)).isEqualTo(AnaliticaService.Resultado.REGISTRADO);

        verify(eventos).save(argThat((EventoCita e) -> e.getParticion() == 4 && e.getOffsetKafka() == 120L
                && e.getEventoId().equals("e1")));
    }

    @Test
    void eventoRepetidoNoSeCuentaDosVeces() {
        when(eventos.existsByEventoId("e1")).thenReturn(true);

        assertThat(servicio.registrarEventoCita(evento("e1"), 0, 1L)).isEqualTo(AnaliticaService.Resultado.DUPLICADO);

        verify(eventos, never()).save(any());
    }

    @Test
    void estadoDeUsuarioNuevoSeRegistra() {
        when(usuarios.findById(7L)).thenReturn(Optional.empty());

        assertThat(servicio.aplicarEstadoUsuario(estado("u1", true, T0))).isEqualTo(AnaliticaService.Resultado.REGISTRADO);

        verify(usuarios).save(any(UsuarioEstado.class));
    }

    @Test
    void estadoMasRecienteReemplazaAlGuardado() {
        UsuarioEstado guardado = UsuarioEstado.desde(estado("u1", true, T0), T0);
        when(usuarios.findById(7L)).thenReturn(Optional.of(guardado));

        assertThat(servicio.aplicarEstadoUsuario(estado("u2", false, T0.plusMinutes(5))))
                .isEqualTo(AnaliticaService.Resultado.ACTUALIZADO);

        assertThat(guardado.isActivo()).isFalse();
        assertThat(guardado.getUltimoEventoId()).isEqualTo("u2");
    }

    @Test
    void estadoAntiguoODuplicadoSeIgnora() {
        UsuarioEstado guardado = UsuarioEstado.desde(estado("u2", false, T0.plusMinutes(5)), T0);
        when(usuarios.findById(7L)).thenReturn(Optional.of(guardado));

        assertThat(servicio.aplicarEstadoUsuario(estado("u1", true, T0)))
                .isEqualTo(AnaliticaService.Resultado.IGNORADO_ANTIGUO);
        assertThat(servicio.aplicarEstadoUsuario(estado("u2", false, T0.plusMinutes(5))))
                .isEqualTo(AnaliticaService.Resultado.DUPLICADO);
        assertThat(guardado.isActivo()).isFalse();
    }

    @Test
    void tombstoneEliminaAlUsuario() {
        when(usuarios.existsById(7L)).thenReturn(true);
        when(usuarios.existsById(8L)).thenReturn(false);

        assertThat(servicio.eliminarUsuario(7L)).isEqualTo(AnaliticaService.Resultado.ELIMINADO);
        assertThat(servicio.eliminarUsuario(8L)).isEqualTo(AnaliticaService.Resultado.SIN_CAMBIOS);

        verify(usuarios).deleteById(7L);
        verify(usuarios, never()).deleteById(8L);
    }

    @Test
    void resumenCalculaTotalesYTasaDeCancelacion() {
        when(eventos.contarPorTipo()).thenReturn(List.of(conteo("CREADA", 8), conteo("CANCELADA", 2), conteo("CONFIRMADA", 5)));
        when(eventos.contarPorEspecialidad(anyString())).thenReturn(List.of(conteo("Medicina General", 8)));
        when(eventos.contarPorMedico(anyString())).thenReturn(List.of(conteo("Dra. Rojas", 8)));
        UsuarioEstado ana = UsuarioEstado.desde(estado("u1", true, T0), T0);
        UsuarioEstado inactivo = UsuarioEstado.desde(new UsuarioEstadoMsg("u9", "DESACTIVADO", 9L, "m@demo.cl", "M",
                List.of("Medico", "Paciente"), false, T0), T0);
        when(usuarios.findAll()).thenReturn(List.of(ana, inactivo));

        ResumenAnaliticaResponse r = servicio.resumen();

        assertThat(r.totalEventos()).isEqualTo(15);
        assertThat(r.eventosPorTipo()).containsEntry("CREADA", 8L).containsEntry("CANCELADA", 2L);
        assertThat(r.tasaCancelacion()).isEqualTo(25.0);
        assertThat(r.reservasPorEspecialidad()).singleElement().satisfies(c -> assertThat(c.total()).isEqualTo(8));
        assertThat(r.usuarios().total()).isEqualTo(2);
        assertThat(r.usuarios().activos()).isEqualTo(1);
        assertThat(r.usuarios().inactivos()).isEqualTo(1);
        assertThat(r.usuarios().porRol()).containsEntry("Paciente", 2L).containsEntry("Medico", 1L);
        assertThat(r.generadoEn()).isEqualTo(LocalDateTime.of(2026, 9, 28, 9, 0));
    }

    @Test
    void sinCitasCreadasLaTasaEsCero() {
        when(eventos.contarPorTipo()).thenReturn(List.of());
        when(usuarios.findAll()).thenReturn(List.of());

        assertThat(servicio.resumen().tasaCancelacion()).isZero();
    }

    private static ConteoPorClave conteo(String clave, long total) {
        return new ConteoPorClave() {
            @Override
            public String getClave() {
                return clave;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }
}

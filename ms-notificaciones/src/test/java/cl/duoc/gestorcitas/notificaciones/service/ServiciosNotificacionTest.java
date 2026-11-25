package cl.duoc.gestorcitas.notificaciones.service;

import cl.duoc.gestorcitas.notificaciones.config.ComprobantesProperties;
import cl.duoc.gestorcitas.notificaciones.config.SimulacionProperties;
import cl.duoc.gestorcitas.notificaciones.entity.AuditoriaUsuario;
import cl.duoc.gestorcitas.notificaciones.entity.Comprobante;
import cl.duoc.gestorcitas.notificaciones.entity.NotificacionEnviada;
import cl.duoc.gestorcitas.notificaciones.exception.ErrorTransitorioException;
import cl.duoc.gestorcitas.notificaciones.exception.MensajeInvalidoException;
import cl.duoc.gestorcitas.notificaciones.messaging.event.CitaEvento;
import cl.duoc.gestorcitas.notificaciones.messaging.event.SolicitudComprobante;
import cl.duoc.gestorcitas.notificaciones.messaging.event.UsuarioEvento;
import cl.duoc.gestorcitas.notificaciones.repository.AuditoriaUsuarioRepository;
import cl.duoc.gestorcitas.notificaciones.repository.ComprobanteRepository;
import cl.duoc.gestorcitas.notificaciones.repository.NotificacionEnviadaRepository;
import cl.duoc.gestorcitas.notificaciones.service.impl.AuditoriaServiceImpl;
import cl.duoc.gestorcitas.notificaciones.service.impl.ComprobanteServiceImpl;
import cl.duoc.gestorcitas.notificaciones.service.impl.NotificacionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiciosNotificacionTest {

    private final NotificacionEnviadaRepository notificacionRepo = mock(NotificacionEnviadaRepository.class);
    private final NotificacionService notificaciones =
            new NotificacionServiceImpl(new SimulacionProperties("falla.test"), notificacionRepo);

    // ------------------------------------------------------------------ Notificaciones

    @Test
    void notificaCitaCreadaYLaGuardaEnBaseDeDatos() {
        notificaciones.notificarCambioDeCita(evento("CREADA", "ana@demo.cl"));

        ArgumentCaptor<NotificacionEnviada> guardada = ArgumentCaptor.forClass(NotificacionEnviada.class);
        verify(notificacionRepo).save(guardada.capture());
        assertThat(guardada.getValue().getEventoId()).isEqualTo("e-1");
        assertThat(guardada.getValue().getCitaId()).isEqualTo(10L);
        assertThat(guardada.getValue().getDestinatario()).isEqualTo("ana@demo.cl");
        assertThat(guardada.getValue().getAsunto()).isEqualTo("Recibimos tu reserva");
    }

    @Test
    void eventoRepetidoNoEnviaNiGuardaDeNuevo() {
        when(notificacionRepo.existsByEventoId("e-1")).thenReturn(true);

        notificaciones.notificarCambioDeCita(evento("CREADA", "ana@demo.cl"));

        verify(notificacionRepo, never()).save(any());
    }

    @Test
    void dominioDeCorreoCaidoEsErrorRecuperableYNoGuarda() {
        assertThatThrownBy(() -> notificaciones.notificarCambioDeCita(evento("CREADA", "ana@falla.test")))
                .isInstanceOf(ErrorTransitorioException.class);
        verify(notificacionRepo, never()).save(any());
    }

    @Test
    void tipoDeEventoDesconocidoEsErrorNoRecuperable() {
        assertThatThrownBy(() -> notificaciones.notificarCambioDeCita(evento("REPROGRAMADA", "ana@demo.cl")))
                .isInstanceOf(MensajeInvalidoException.class);
        verify(notificacionRepo, never()).save(any());
    }

    // ------------------------------------------------------------------ Comprobantes

    @Test
    void generaComprobanteEnArchivoYLoGuarda(@TempDir Path dir) throws Exception {
        ComprobanteRepository repo = mock(ComprobanteRepository.class);
        when(repo.findBySolicitudId("s-1")).thenReturn(Optional.empty());
        ComprobanteService comprobantes = new ComprobanteServiceImpl(new ComprobantesProperties(dir.toString()), repo);

        Path archivo = comprobantes.generar(solicitud());

        assertThat(archivo.getFileName().toString()).isEqualTo("comprobante-GC-20260929-10.txt");
        assertThat(Files.readString(archivo)).contains("Ana", "Dra. Rojas", "29-09-2026 10:00");
        ArgumentCaptor<Comprobante> guardado = ArgumentCaptor.forClass(Comprobante.class);
        verify(repo).save(guardado.capture());
        assertThat(guardado.getValue().getCodigo()).isEqualTo("GC-20260929-10");
        assertThat(guardado.getValue().getSolicitudId()).isEqualTo("s-1");
        assertThat(guardado.getValue().getArchivo()).isEqualTo(archivo.toString());
    }

    @Test
    void solicitudRepetidaDevuelveElComprobanteExistente(@TempDir Path dir) {
        ComprobanteRepository repo = mock(ComprobanteRepository.class);
        Comprobante existente = Comprobante.desde(solicitud(), "GC-20260929-10", "/data/x.txt", LocalDateTime.now());
        when(repo.findBySolicitudId("s-1")).thenReturn(Optional.of(existente));
        ComprobanteService comprobantes = new ComprobanteServiceImpl(new ComprobantesProperties(dir.toString()), repo);

        Path archivo = comprobantes.generar(solicitud());

        assertThat(archivo).isEqualTo(Path.of("/data/x.txt"));
        verify(repo, never()).save(any());
    }

    // ------------------------------------------------------------------ Auditoría

    @Test
    void registraAuditoriaEnBaseDeDatos() {
        AuditoriaUsuarioRepository repo = mock(AuditoriaUsuarioRepository.class);
        AuditoriaService auditoria = new AuditoriaServiceImpl(repo);

        auditoria.registrar(usuarioEvento());

        ArgumentCaptor<AuditoriaUsuario> guardada = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(repo).save(guardada.capture());
        assertThat(guardada.getValue().getEventoId()).isEqualTo("u-1");
        assertThat(guardada.getValue().getRoles()).isEqualTo("Paciente,Admin");
        assertThat(guardada.getValue().isActivo()).isTrue();
    }

    @Test
    void auditoriaRepetidaNoSeGuardaDosVeces() {
        AuditoriaUsuarioRepository repo = mock(AuditoriaUsuarioRepository.class);
        when(repo.existsByEventoId("u-1")).thenReturn(true);

        new AuditoriaServiceImpl(repo).registrar(usuarioEvento());

        verify(repo, never()).save(any());
    }

    // ------------------------------------------------------------------ Datos de prueba

    private static CitaEvento evento(String tipo, String email) {
        return new CitaEvento("e-1", tipo, 10L, "oid", "Ana", email, "Dra. Rojas", null,
                "Medicina General", LocalDateTime.of(2026, 9, 29, 10, 0), "PENDIENTE", null);
    }

    private static SolicitudComprobante solicitud() {
        return new SolicitudComprobante("s-1", 10L, "Ana", "ana@demo.cl",
                "Dra. Rojas", "Medicina General", LocalDateTime.of(2026, 9, 29, 10, 0), "Control", null);
    }

    private static UsuarioEvento usuarioEvento() {
        return new UsuarioEvento("u-1", "REGISTRADO", 7L, "ana@demo.cl", "Ana",
                List.of("Paciente", "Admin"), true, "oid-admin", LocalDateTime.of(2026, 9, 29, 9, 0));
    }
}

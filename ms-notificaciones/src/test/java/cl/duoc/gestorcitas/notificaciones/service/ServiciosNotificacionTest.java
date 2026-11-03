package cl.duoc.gestorcitas.notificaciones.service;

import cl.duoc.gestorcitas.notificaciones.config.ComprobantesProperties;
import cl.duoc.gestorcitas.notificaciones.config.SimulacionProperties;
import cl.duoc.gestorcitas.notificaciones.exception.ErrorTransitorioException;
import cl.duoc.gestorcitas.notificaciones.exception.MensajeInvalidoException;
import cl.duoc.gestorcitas.notificaciones.messaging.event.CitaEvento;
import cl.duoc.gestorcitas.notificaciones.messaging.event.SolicitudComprobante;
import cl.duoc.gestorcitas.notificaciones.service.impl.ComprobanteServiceImpl;
import cl.duoc.gestorcitas.notificaciones.service.impl.NotificacionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiciosNotificacionTest {

    private final NotificacionService notificaciones =
            new NotificacionServiceImpl(new SimulacionProperties("falla.test"));

    @Test
    void notificaCitaCreada() {
        assertThatNoException().isThrownBy(() -> notificaciones.notificarCambioDeCita(evento("CREADA", "ana@demo.cl")));
    }

    @Test
    void dominioDeCorreoCaidoEsErrorRecuperable() {
        assertThatThrownBy(() -> notificaciones.notificarCambioDeCita(evento("CREADA", "ana@falla.test")))
                .isInstanceOf(ErrorTransitorioException.class);
    }

    @Test
    void tipoDeEventoDesconocidoEsErrorNoRecuperable() {
        assertThatThrownBy(() -> notificaciones.notificarCambioDeCita(evento("REPROGRAMADA", "ana@demo.cl")))
                .isInstanceOf(MensajeInvalidoException.class);
    }

    @Test
    void generaComprobanteEnArchivo(@TempDir Path dir) throws Exception {
        ComprobanteService comprobantes = new ComprobanteServiceImpl(new ComprobantesProperties(dir.toString()));

        Path archivo = comprobantes.generar(new SolicitudComprobante("s-1", 10L, "Ana", "ana@demo.cl",
                "Dra. Rojas", "Medicina General", LocalDateTime.of(2026, 9, 29, 10, 0), "Control", null));

        assertThat(archivo.getFileName().toString()).isEqualTo("comprobante-GC-20260929-10.txt");
        assertThat(Files.readString(archivo)).contains("Ana", "Dra. Rojas", "29-09-2026 10:00");
    }

    private static CitaEvento evento(String tipo, String email) {
        return new CitaEvento("e-1", tipo, 10L, "oid", "Ana", email, "Dra. Rojas", null,
                "Medicina General", LocalDateTime.of(2026, 9, 29, 10, 0), "PENDIENTE", null);
    }
}

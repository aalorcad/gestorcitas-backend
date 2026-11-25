package cl.duoc.gestorcitas.notificaciones;

import cl.duoc.gestorcitas.notificaciones.entity.AuditoriaUsuario;
import cl.duoc.gestorcitas.notificaciones.entity.NotificacionEnviada;
import cl.duoc.gestorcitas.notificaciones.messaging.event.CitaEvento;
import cl.duoc.gestorcitas.notificaciones.messaging.event.UsuarioEvento;
import cl.duoc.gestorcitas.notificaciones.repository.AuditoriaUsuarioRepository;
import cl.duoc.gestorcitas.notificaciones.repository.NotificacionEnviadaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifica el mapeo JPA y la restricción única que evita duplicados (H2 en modo Oracle). */
@DataJpaTest
class PersistenciaNotificacionesTest {

    @Autowired
    private NotificacionEnviadaRepository notificaciones;

    @Autowired
    private AuditoriaUsuarioRepository auditoria;

    @Test
    void guardaYEncuentraUnaNotificacionPorEventoId() {
        notificaciones.saveAndFlush(NotificacionEnviada.desde(cita("e-1"), "Recibimos tu reserva", LocalDateTime.now()));

        assertThat(notificaciones.existsByEventoId("e-1")).isTrue();
        assertThat(notificaciones.existsByEventoId("e-2")).isFalse();
    }

    @Test
    void elMismoEventoNoSePuedeGuardarDosVeces() {
        auditoria.saveAndFlush(AuditoriaUsuario.desde(usuario("u-1"), LocalDateTime.now()));

        assertThatThrownBy(() -> auditoria.saveAndFlush(AuditoriaUsuario.desde(usuario("u-1"), LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static CitaEvento cita(String eventoId) {
        return new CitaEvento(eventoId, "CREADA", 10L, "oid", "Ana", "ana@demo.cl", "Dra. Rojas", null,
                "Medicina General", LocalDateTime.of(2026, 9, 29, 10, 0), "PENDIENTE", null);
    }

    private static UsuarioEvento usuario(String eventoId) {
        return new UsuarioEvento(eventoId, "REGISTRADO", 7L, "ana@demo.cl", "Ana",
                List.of("Paciente"), true, null, LocalDateTime.now());
    }
}

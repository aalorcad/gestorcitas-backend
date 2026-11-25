package cl.duoc.gestorcitas.usuarios.messaging;

import cl.duoc.gestorcitas.usuarios.entity.RolUsuario;
import cl.duoc.gestorcitas.usuarios.entity.Usuario;
import cl.duoc.gestorcitas.usuarios.messaging.kafka.KafkaEventSender;
import cl.duoc.gestorcitas.usuarios.messaging.kafka.KafkaStreamingProperties;
import cl.duoc.gestorcitas.usuarios.messaging.kafka.KafkaUsuarioStreamPublisher;
import cl.duoc.gestorcitas.usuarios.messaging.kafka.UsuarioEstadoKafka;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Verifica el tópico compactado, la clave (usuarioId) y que se publica el estado completo del usuario. */
class KafkaUsuarioStreamPublisherTest {

    private static final KafkaStreamingProperties PROPS =
            new KafkaStreamingProperties(true, new KafkaStreamingProperties.Topicos("gestorcitas.usuarios.estado"));

    private final KafkaEventSender sender = mock(KafkaEventSender.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneId.of("America/Santiago"));

    private final Usuario usuario = Usuario.builder().id(7L).email("ana@demo.cl").nombre("Ana")
            .roles(EnumSet.of(RolUsuario.PACIENTE)).activo(false).build();

    @Test
    void publicaElEstadoConClaveUsuarioId() {
        new KafkaUsuarioStreamPublisher(sender, PROPS, clock).publicar(usuario, TipoEventoUsuario.DESACTIVADO);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<String> eventoId = ArgumentCaptor.forClass(String.class);
        verify(sender).enviarTrasCommit(eq("gestorcitas.usuarios.estado"), eq("7"), payload.capture(), eventoId.capture());

        UsuarioEstadoKafka estado = (UsuarioEstadoKafka) payload.getValue();
        assertThat(estado.eventoId()).isEqualTo(eventoId.getValue()).isNotBlank();
        assertThat(estado.tipo()).isEqualTo(TipoEventoUsuario.DESACTIVADO);
        assertThat(estado.usuarioId()).isEqualTo(7L);
        assertThat(estado.activo()).isFalse();
        assertThat(estado.roles()).containsExactly("Paciente");
        assertThat(estado.actualizadoEn()).isEqualTo(LocalDateTime.of(2026, 9, 28, 9, 0));
    }

    @Test
    void conKafkaDeshabilitadoNoSePublica() {
        KafkaStreamingProperties apagado = new KafkaStreamingProperties(false, PROPS.topicos());

        new KafkaUsuarioStreamPublisher(sender, apagado, clock).publicar(usuario, TipoEventoUsuario.REGISTRADO);

        verifyNoInteractions(sender);
    }
}

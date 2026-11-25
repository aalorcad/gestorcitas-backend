package cl.duoc.gestorcitas.citas.messaging;

import cl.duoc.gestorcitas.citas.entity.Cita;
import cl.duoc.gestorcitas.citas.entity.EstadoCita;
import cl.duoc.gestorcitas.citas.messaging.kafka.CitaEventoKafka;
import cl.duoc.gestorcitas.citas.messaging.kafka.KafkaCitaStreamPublisher;
import cl.duoc.gestorcitas.citas.messaging.kafka.KafkaEventSender;
import cl.duoc.gestorcitas.citas.messaging.kafka.KafkaStreamingProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Verifica el tópico, la clave (citaId) y el contenido del evento publicado en Kafka. */
class KafkaCitaStreamPublisherTest {

    private static final KafkaStreamingProperties PROPS =
            new KafkaStreamingProperties(true, new KafkaStreamingProperties.Topicos("gestorcitas.citas.eventos"));

    private final KafkaEventSender sender = mock(KafkaEventSender.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneId.of("America/Santiago"));

    private final Cita cita = Cita.builder().id(42L).pacienteId("oid").medicoId(3L).medicoNombre("Dra. Rojas")
            .especialidadId(10L).especialidadNombre("Medicina General")
            .fechaHora(LocalDateTime.of(2026, 9, 29, 10, 0)).estado(EstadoCita.CONFIRMADA).build();

    @Test
    void publicaEnElTopicoConClaveCitaId() {
        new KafkaCitaStreamPublisher(sender, PROPS, clock).publicar(cita, TipoEventoCita.CONFIRMADA);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<String> eventoId = ArgumentCaptor.forClass(String.class);
        verify(sender).enviarTrasCommit(eq("gestorcitas.citas.eventos"), eq("42"), payload.capture(), eventoId.capture());

        CitaEventoKafka evento = (CitaEventoKafka) payload.getValue();
        assertThat(evento.eventoId()).isEqualTo(eventoId.getValue()).isNotBlank();
        assertThat(evento.tipo()).isEqualTo(TipoEventoCita.CONFIRMADA);
        assertThat(evento.citaId()).isEqualTo(42L);
        assertThat(evento.medicoId()).isEqualTo(3L);
        assertThat(evento.especialidadId()).isEqualTo(10L);
        assertThat(evento.estado()).isEqualTo("CONFIRMADA");
        assertThat(evento.ocurridoEn()).isEqualTo(LocalDateTime.of(2026, 9, 28, 9, 0));
    }

    @Test
    void conKafkaDeshabilitadoNoSePublica() {
        KafkaStreamingProperties apagado = new KafkaStreamingProperties(false, PROPS.topicos());

        new KafkaCitaStreamPublisher(sender, apagado, clock).publicar(cita, TipoEventoCita.CREADA);

        verifyNoInteractions(sender);
    }
}

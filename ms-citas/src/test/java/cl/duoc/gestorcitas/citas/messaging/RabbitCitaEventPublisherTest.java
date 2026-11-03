package cl.duoc.gestorcitas.citas.messaging;

import cl.duoc.gestorcitas.citas.entity.Cita;
import cl.duoc.gestorcitas.citas.entity.EstadoCita;
import cl.duoc.gestorcitas.citas.messaging.config.MessagingProperties;
import cl.duoc.gestorcitas.citas.messaging.event.CitaEvento;
import cl.duoc.gestorcitas.citas.messaging.event.SolicitudComprobante;
import cl.duoc.gestorcitas.citas.messaging.producer.RabbitCitaEventPublisher;
import cl.duoc.gestorcitas.citas.messaging.producer.RabbitEventSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Verifica que cada caso de uso publica en el exchange y routing key definidos en la configuración. */
class RabbitCitaEventPublisherTest {

    private static final MessagingProperties PROPS = new MessagingProperties(true,
            new MessagingProperties.Exchanges("ex.eventos", "ex.comprobantes"),
            new MessagingProperties.RoutingKeys("cita.creada", "cita.confirmada", "cita.cancelada",
                    "cita.atendida", "comprobante.generar"));

    private final RabbitEventSender sender = mock(RabbitEventSender.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneId.of("America/Santiago"));

    private final Cita cita = Cita.builder().id(10L).pacienteId("oid").pacienteNombre("Ana")
            .pacienteEmail("ana@demo.cl").medicoNombre("Dra. Rojas").medicoEmail("rojas@demo.cl")
            .especialidadNombre("Medicina General").fechaHora(LocalDateTime.of(2026, 9, 29, 10, 0))
            .estado(EstadoCita.CONFIRMADA).motivo("Control").build();

    @Test
    void cambioDeEstadoUsaExchangeTopicYRoutingKeyDelEvento() {
        new RabbitCitaEventPublisher(sender, PROPS, clock).publicarCambioDeEstado(cita, TipoEventoCita.CONFIRMADA);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(sender).enviarTrasCommit(eq("ex.eventos"), eq("cita.confirmada"), payload.capture(), anyString());
        CitaEvento evento = (CitaEvento) payload.getValue();
        assertThat(evento.citaId()).isEqualTo(10L);
        assertThat(evento.tipo()).isEqualTo(TipoEventoCita.CONFIRMADA);
        assertThat(evento.pacienteEmail()).isEqualTo("ana@demo.cl");
    }

    @Test
    void comprobanteUsaExchangeDirect() {
        new RabbitCitaEventPublisher(sender, PROPS, clock).solicitarComprobante(cita);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(sender).enviarTrasCommit(eq("ex.comprobantes"), eq("comprobante.generar"), payload.capture(), anyString());
        assertThat(((SolicitudComprobante) payload.getValue()).motivo()).isEqualTo("Control");
    }

    @Test
    void conMensajeriaDeshabilitadaNoSePublica() {
        MessagingProperties apagado = new MessagingProperties(false, PROPS.exchanges(), PROPS.routingKeys());
        RabbitCitaEventPublisher publisher = new RabbitCitaEventPublisher(sender, apagado, clock);

        publisher.publicarCambioDeEstado(cita, TipoEventoCita.CREADA);
        publisher.solicitarComprobante(cita);

        verifyNoInteractions(sender);
    }
}

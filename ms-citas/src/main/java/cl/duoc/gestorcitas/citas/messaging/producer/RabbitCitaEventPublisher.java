package cl.duoc.gestorcitas.citas.messaging.producer;

import cl.duoc.gestorcitas.citas.entity.Cita;
import cl.duoc.gestorcitas.citas.messaging.CitaEventPublisher;
import cl.duoc.gestorcitas.citas.messaging.TipoEventoCita;
import cl.duoc.gestorcitas.citas.messaging.config.MessagingProperties;
import cl.duoc.gestorcitas.citas.messaging.event.CitaEvento;
import cl.duoc.gestorcitas.citas.messaging.event.SolicitudComprobante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Productor RabbitMQ de ms-citas: traduce lo ocurrido en el negocio a mensajes.
 * Los nombres de exchanges y routing keys salen de {@link MessagingProperties}.
 */
@Component
public class RabbitCitaEventPublisher implements CitaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitCitaEventPublisher.class);

    private final RabbitEventSender sender;
    private final MessagingProperties props;
    private final Clock clock;

    public RabbitCitaEventPublisher(RabbitEventSender sender, MessagingProperties props, Clock clock) {
        this.sender = sender;
        this.props = props;
        this.clock = clock;
    }

    @Override
    public void publicarCambioDeEstado(Cita cita, TipoEventoCita tipo) {
        if (!props.habilitado()) {
            log.debug("Mensajería deshabilitada: no se publica evento {} de la cita {}", tipo, cita.getId());
            return;
        }
        CitaEvento evento = CitaEvento.desde(cita, tipo, LocalDateTime.now(clock));
        sender.enviarTrasCommit(
                props.exchanges().citasEventos(),
                props.routingKeys().paraEvento(tipo),
                evento,
                evento.eventoId());
    }

    @Override
    public void solicitarComprobante(Cita cita) {
        if (!props.habilitado()) {
            log.debug("Mensajería deshabilitada: no se solicita comprobante de la cita {}", cita.getId());
            return;
        }
        SolicitudComprobante solicitud = SolicitudComprobante.desde(cita, LocalDateTime.now(clock));
        sender.enviarTrasCommit(
                props.exchanges().citasComprobantes(),
                props.routingKeys().comprobanteGenerar(),
                solicitud,
                solicitud.solicitudId());
    }
}

package cl.duoc.gestorcitas.citas.messaging.kafka;

import cl.duoc.gestorcitas.citas.entity.Cita;
import cl.duoc.gestorcitas.citas.messaging.CitaStreamPublisher;
import cl.duoc.gestorcitas.citas.messaging.TipoEventoCita;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Productor Kafka de ms-citas: publica cada cambio de estado de una cita en
 * {@code app.kafka.topicos.citas-eventos} con clave {@code citaId} (orden garantizado por cita).
 */
@Component
public class KafkaCitaStreamPublisher implements CitaStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaCitaStreamPublisher.class);

    private final KafkaEventSender sender;
    private final KafkaStreamingProperties props;
    private final Clock clock;

    public KafkaCitaStreamPublisher(KafkaEventSender sender, KafkaStreamingProperties props, Clock clock) {
        this.sender = sender;
        this.props = props;
        this.clock = clock;
    }

    @Override
    public void publicar(Cita cita, TipoEventoCita tipo) {
        if (!props.habilitado()) {
            log.debug("Kafka deshabilitado: no se publica evento {} de la cita {}", tipo, cita.getId());
            return;
        }
        CitaEventoKafka evento = CitaEventoKafka.desde(cita, tipo, LocalDateTime.now(clock));
        sender.enviarTrasCommit(
                props.topicos().citasEventos(),
                String.valueOf(cita.getId()),
                evento,
                evento.eventoId());
    }
}

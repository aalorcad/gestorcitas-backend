package cl.duoc.gestorcitas.notificaciones.messaging.kafka;

import cl.duoc.gestorcitas.notificaciones.exception.MensajeInvalidoException;
import cl.duoc.gestorcitas.notificaciones.service.HistorialCitaService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consumidor Kafka del grupo "historial-citas".
 * <p>
 * Lee el MISMO tópico que ms-analitica pero con otro grupo de consumidores: Kafka entrega todos los
 * eventos a cada grupo y cada uno avanza su propio offset (a diferencia de una cola RabbitMQ, donde un
 * mensaje consumido desaparece). Errores: ver {@link KafkaHistorialConfig}.
 */
@Component
public class HistorialCitaKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(HistorialCitaKafkaConsumer.class);

    private final HistorialCitaService historial;
    private final Validator validator;

    public HistorialCitaKafkaConsumer(HistorialCitaService historial, Validator validator) {
        this.historial = historial;
        this.validator = validator;
    }

    @KafkaListener(
            id = "historial-citas-eventos",
            groupId = "${app.kafka.grupo}",
            autoStartup = "${app.kafka.habilitado:true}",
            topics = "${app.kafka.topicos.citas-eventos}",
            properties = "spring.json.value.default.type=cl.duoc.gestorcitas.notificaciones.messaging.kafka.CitaEventoKafkaMsg")
    public void consumir(ConsumerRecord<String, CitaEventoKafkaMsg> registro) {
        String contexto = "topico=" + registro.topic() + " particion=" + registro.partition()
                + " offset=" + registro.offset() + " clave=" + registro.key();
        CitaEventoKafkaMsg evento = registro.value();
        validar(evento, contexto);

        if (historial.registrar(evento, registro.partition(), registro.offset())) {
            log.info("[Kafka][historial-citas] Cita {} -> {} (eventoId={}) {}",
                    evento.citaId(), evento.tipo(), evento.eventoId(), contexto);
        } else {
            log.warn("[Kafka][historial-citas][DUPLICADO] Evento {} ya registrado; se ignora {}", evento.eventoId(), contexto);
        }
    }

    private void validar(CitaEventoKafkaMsg evento, String contexto) {
        if (evento == null) {
            throw new MensajeInvalidoException("Registro sin contenido " + contexto);
        }
        Set<ConstraintViolation<CitaEventoKafkaMsg>> errores = validator.validate(evento);
        if (!errores.isEmpty()) {
            String detalle = errores.stream().map(e -> e.getPropertyPath() + " " + e.getMessage())
                    .sorted().collect(Collectors.joining("; "));
            throw new MensajeInvalidoException("Evento inválido [" + detalle + "] " + contexto);
        }
    }
}

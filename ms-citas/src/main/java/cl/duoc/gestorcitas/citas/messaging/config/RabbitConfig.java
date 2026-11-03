package cl.duoc.gestorcitas.citas.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración RabbitMQ del PRODUCTOR ms-citas.
 * <p>
 * Casos de uso que publica este microservicio:
 * <ol>
 *   <li><b>Notificación de cambios de estado</b>: exchange TOPIC {@code app.messaging.exchanges.citas-eventos},
 *       routing keys {@code cita.creada | cita.confirmada | cita.cancelada | cita.atendida}.</li>
 *   <li><b>Generación de comprobante</b>: exchange DIRECT {@code app.messaging.exchanges.citas-comprobantes},
 *       routing key {@code comprobante.generar}.</li>
 * </ol>
 * El productor declara solo los exchanges donde publica. Las colas, bindings y DLQ los declara el
 * consumidor (ms-notificaciones), que es quien conoce cómo procesa cada mensaje.
 */
@Configuration
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    private final MessagingProperties props;

    public RabbitConfig(MessagingProperties props) {
        this.props = props;
    }

    // ------------------------------------------------------------------ Exchanges

    @Bean
    public TopicExchange citasEventosExchange() {
        return new TopicExchange(props.exchanges().citasEventos(), true, false);
    }

    @Bean
    public DirectExchange citasComprobantesExchange() {
        return new DirectExchange(props.exchanges().citasComprobantes(), true, false);
    }

    // ------------------------------------------------------------------ Conversión y plantilla

    /** Los mensajes viajan como JSON usando el mismo ObjectMapper de la API (fechas ISO-8601). */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate con:
     * <ul>
     *   <li>mandatory=true: si ningún binding acepta el mensaje, el broker lo devuelve (ReturnsCallback).</li>
     *   <li>ConfirmCallback: el broker confirma (ACK) o rechaza (NACK) cada publicación.</li>
     * </ul>
     * Ambos casos de "mensaje no entregado" quedan registrados en el log.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlation, ack, cause) -> {
            String id = correlation == null ? "?" : correlation.getId();
            if (ack) {
                log.debug("[RabbitMQ] Broker confirmó el mensaje id={}", id);
            } else {
                log.error("[RabbitMQ] Broker RECHAZÓ el mensaje id={} causa={}", id, cause);
            }
        });
        template.setReturnsCallback(returned -> log.error(
                "[RabbitMQ] Mensaje NO ENRUTADO y devuelto: exchange={} routingKey={} código={} motivo={} id={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(),
                returned.getReplyText(), returned.getMessage().getMessageProperties().getMessageId()));
        return template;
    }
}

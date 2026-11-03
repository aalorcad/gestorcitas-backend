package cl.duoc.gestorcitas.usuarios.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración RabbitMQ del PRODUCTOR ms-usuarios.
 * <p>
 * Caso de uso: <b>auditoría de usuarios</b>. Exchange TOPIC {@code app.messaging.exchanges.usuarios-eventos}
 * con routing keys {@code usuario.registrado | usuario.activado | usuario.desactivado}.
 * La cola de auditoría y su DLQ las declara ms-notificaciones (el consumidor).
 */
@Configuration
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    private final MessagingProperties props;

    public RabbitConfig(MessagingProperties props) {
        this.props = props;
    }

    @Bean
    public TopicExchange usuariosEventosExchange() {
        return new TopicExchange(props.exchanges().usuariosEventos(), true, false);
    }

    /** Los mensajes viajan como JSON usando el mismo ObjectMapper de la API (fechas ISO-8601). */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /** RabbitTemplate con confirmaciones del broker y devolución de mensajes no enrutables (mandatory). */
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

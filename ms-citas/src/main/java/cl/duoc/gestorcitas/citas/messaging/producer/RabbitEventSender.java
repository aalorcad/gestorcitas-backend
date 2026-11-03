package cl.duoc.gestorcitas.citas.messaging.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Envío técnico de mensajes a RabbitMQ.
 * <ul>
 *   <li>Si hay una transacción activa, el envío se difiere hasta DESPUÉS del commit: nunca se avisa
 *       de una cita que finalmente no se guardó.</li>
 *   <li>Los errores del broker se registran y NO se propagan: la reserva ya está confirmada en la base
 *       de datos y no debe fallar porque RabbitMQ no esté disponible.</li>
 * </ul>
 */
@Component
public class RabbitEventSender {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventSender.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviarTrasCommit(String exchange, String routingKey, Object payload, String mensajeId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(exchange, routingKey, payload, mensajeId);
                }
            });
        } else {
            enviar(exchange, routingKey, payload, mensajeId);
        }
    }

    void enviar(String exchange, String routingKey, Object payload, String mensajeId) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
                message.getMessageProperties().setMessageId(mensajeId);
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return message;
            }, new CorrelationData(mensajeId));
            log.info("[RabbitMQ] Publicado exchange={} routingKey={} id={}", exchange, routingKey, mensajeId);
        } catch (AmqpException ex) {
            log.error("[RabbitMQ] No se pudo publicar exchange={} routingKey={} id={}. "
                    + "La operación de negocio se mantiene. Causa: {}", exchange, routingKey, mensajeId, ex.getMessage());
        }
    }
}

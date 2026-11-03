package cl.duoc.gestorcitas.notificaciones.messaging.support;

import cl.duoc.gestorcitas.notificaciones.exception.MensajeInvalidoException;
import cl.duoc.gestorcitas.notificaciones.messaging.config.MessagingProperties;
import cl.duoc.gestorcitas.notificaciones.messaging.config.MessagingProperties.Flujo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Política ÚNICA de confirmación (ACK) y manejo de errores para todos los consumidores.
 *
 * <pre>
 *  Resultado del procesamiento            Acción                                        Destino del mensaje
 *  -------------------------------------  --------------------------------------------  ---------------------------
 *  OK                                     basicAck                                      se elimina de la cola
 *  MensajeInvalidoException (JSON malo,   basicNack(requeue=false)                      DLQ inmediatamente
 *  campos obligatorios faltantes)         -> el broker lo dead-letterea
 *  Otro error (recuperable) y quedan      se publica una copia en la cola de reintento  vuelve a la cola principal
 *  intentos                               (TTL) con el intento+1 y luego basicAck       después de {@code esperaMs}
 *  Otro error y NO quedan intentos        basicNack(requeue=false)                      DLQ
 * </pre>
 *
 * Nunca se usa {@code requeue=true}: un mensaje que falla siempre no puede quedar dando vueltas en la cola.
 * Cada rechazo hacia la DLQ queda registrado en el log con el motivo y el contenido del mensaje.
 */
@Component
public class ProcesadorDeMensajes {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorDeMensajes.class);
    private static final int MAX_LOG_PAYLOAD = 500;
    private static final String EXCHANGE_POR_DEFECTO = "";

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public ProcesadorDeMensajes(ObjectMapper objectMapper, Validator validator,
                                RabbitTemplate rabbitTemplate, MessagingProperties props) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    /**
     * Convierte, valida y procesa un mensaje, y decide ACK, reintento o DLQ.
     *
     * @param message mensaje crudo recibido por el {@code @RabbitListener}
     * @param channel canal AMQP (necesario para el ACK manual)
     * @param flujo   flujo de mensajería al que pertenece la cola
     * @param tipo    clase del payload esperado
     * @param logica  lógica de negocio a ejecutar con el payload
     */
    public <T> void procesar(Message message, Channel channel, Flujo flujo,
                             Class<T> tipo, Consumer<T> logica) throws IOException {
        MessageProperties mp = message.getMessageProperties();
        long deliveryTag = mp.getDeliveryTag();
        int intento = intentoActual(mp);

        try {
            T payload = convertir(message, tipo);
            logica.accept(payload);
        } catch (MensajeInvalidoException ex) {
            enviarADlq(channel, message, flujo, intento, "NO RECUPERABLE - " + ex.getMessage());
            return;
        } catch (RuntimeException ex) {
            manejarErrorRecuperable(channel, message, flujo, intento, ex);
            return;
        }

        channel.basicAck(deliveryTag, false);
        log.info("[ACK] cola={} id={} intento={}", flujo.cola(), mp.getMessageId(), intento);
    }

    // ------------------------------------------------------------------ Conversión y validación

    private <T> T convertir(Message message, Class<T> tipo) {
        T payload;
        try {
            payload = objectMapper.readValue(message.getBody(), tipo);
        } catch (JsonProcessingException ex) {
            throw new MensajeInvalidoException("JSON inválido para " + tipo.getSimpleName() + ": " + ex.getOriginalMessage(), ex);
        } catch (IOException ex) {
            throw new MensajeInvalidoException("No se pudo leer el mensaje: " + ex.getMessage(), ex);
        }
        if (payload == null) {
            throw new MensajeInvalidoException("El mensaje está vacío");
        }
        Set<ConstraintViolation<T>> errores = validator.validate(payload);
        if (!errores.isEmpty()) {
            String detalle = errores.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new MensajeInvalidoException("Campos inválidos en " + tipo.getSimpleName() + ": " + detalle);
        }
        return payload;
    }

    // ------------------------------------------------------------------ Reintentos y DLQ

    private void manejarErrorRecuperable(Channel channel, Message message, Flujo flujo, int intento,
                                         RuntimeException error) throws IOException {
        int maximo = props.reintentos().maximo();
        if (intento >= maximo) {
            enviarADlq(channel, message, flujo, intento,
                    "REINTENTOS AGOTADOS (" + intento + "/" + maximo + ") - " + error.getMessage());
            return;
        }
        int siguiente = intento + 1;
        try {
            Message copia = MessageBuilder.fromMessage(message)
                    .setHeader(props.reintentos().header(), siguiente)
                    .build();
            rabbitTemplate.send(EXCHANGE_POR_DEFECTO, flujo.colaReintento(), copia);
        } catch (AmqpException ex) {
            log.error("[REINTENTO] No se pudo programar el reintento en {}: {}", flujo.colaReintento(), ex.getMessage());
            enviarADlq(channel, message, flujo, intento, "REINTENTO NO PROGRAMADO - " + error.getMessage());
            return;
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.warn("[REINTENTO] cola={} id={} intento {}/{} falló: {}. Se reintenta en {} ms vía {}",
                flujo.cola(), message.getMessageProperties().getMessageId(), intento, maximo,
                error.getMessage(), props.reintentos().esperaMs(), flujo.colaReintento());
    }

    private void enviarADlq(Channel channel, Message message, Flujo flujo, int intento, String motivo) throws IOException {
        MessageProperties mp = message.getMessageProperties();
        // requeue=false: el broker mueve el mensaje al dead-letter-exchange configurado en la cola -> DLQ
        channel.basicNack(mp.getDeliveryTag(), false, false);
        log.error("[DLQ] Mensaje rechazado -> {} | cola={} id={} routingKey={} intento={} motivo={} payload={}",
                flujo.dlq(), flujo.cola(), mp.getMessageId(), mp.getReceivedRoutingKey(), intento, motivo,
                resumir(message.getBody()));
    }

    private int intentoActual(MessageProperties mp) {
        Object valor = mp.getHeaders().get(props.reintentos().header());
        return valor instanceof Number n ? n.intValue() : 1;
    }

    private static String resumir(byte[] body) {
        if (body == null) {
            return "<vacío>";
        }
        String texto = new String(body, StandardCharsets.UTF_8);
        return texto.length() > MAX_LOG_PAYLOAD ? texto.substring(0, MAX_LOG_PAYLOAD) + "..." : texto;
    }
}

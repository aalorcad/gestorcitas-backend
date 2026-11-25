package cl.duoc.gestorcitas.notificaciones.messaging.kafka;

import cl.duoc.gestorcitas.notificaciones.exception.MensajeInvalidoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo de errores del CONSUMIDOR Kafka de ms-notificaciones (grupo "historial-citas").
 * <ol>
 *   <li><b>Registro corrupto</b> (JSON ilegible): ErrorHandlingDeserializer evita que el contenedor quede
 *       en bucle; el registro va al tópico {@code <tópico>.DLT} sin reintentos.</li>
 *   <li><b>Evento inválido</b> ({@link MensajeInvalidoException}, Bean Validation): error permanente, va a .DLT sin reintentos.</li>
 *   <li><b>Error transitorio</b> (p. ej. Oracle no disponible): se reintenta {@code app.kafka.reintentos.maximo}
 *       veces en total con espera fija; si sigue fallando, va a .DLT.</li>
 * </ol>
 * El registro enviado a .DLT conserva su clave y partición y lleva cabeceras con la excepción, el tópico,
 * la partición y el offset originales (las agrega DeadLetterPublishingRecoverer).
 * Spring Boot asocia automáticamente este {@code DefaultErrorHandler} a la fábrica de listeners.
 */
@Configuration
public class KafkaHistorialConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaHistorialConfig.class);

    /**
     * Plantilla usada SOLO para publicar en los tópicos .DLT. El valor puede ser el objeto ya deserializado
     * (evento inválido, error transitorio) o los bytes originales (JSON corrupto): cada caso usa su serializador.
     */
    @Bean
    public KafkaTemplate<String, Object> plantillaDlt(KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        JsonSerializer<Object> json = new JsonSerializer<>(objectMapper);
        json.setAddTypeInfo(false);
        Map<Class<?>, Serializer<?>> porTipo = new LinkedHashMap<>();
        porTipo.put(byte[].class, new ByteArraySerializer());
        porTipo.put(Object.class, json);

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<String, Object>(
                config, new StringSerializer(), new DelegatingByTypeSerializer(porTipo, true)));
    }

    @Bean
    public DefaultErrorHandler manejadorDeErroresKafka(KafkaTemplate<String, Object> plantillaDlt,
                                                       KafkaHistorialProperties props) {
        // Destino explícito: <tópico><sufijo-dlt> en la MISMA partición (el .DLT tiene las mismas particiones).
        // Sin esto, Spring Kafka usaría su sufijo por defecto, un tópico que no existe (auto-create desactivado).
        DeadLetterPublishingRecoverer haciaDlt = new DeadLetterPublishingRecoverer(plantillaDlt,
                (registro, ex) -> new TopicPartition(registro.topic() + props.sufijoDlt(), registro.partition()));
        ConsumerRecordRecoverer recuperador = (registro, ex) -> {
            log.error("[Kafka][DLT] Registro enviado a {}{} topico={} particion={} offset={} clave={} causa={}",
                    registro.topic(), props.sufijoDlt(), registro.topic(), registro.partition(), registro.offset(),
                    registro.key(), causa(ex));
            haciaDlt.accept(registro, ex);
        };

        long reintentos = Math.max(0, props.reintentos().maximo() - 1L);
        DefaultErrorHandler manejador = new DefaultErrorHandler(recuperador,
                new FixedBackOff(props.reintentos().esperaMs(), reintentos));
        manejador.addNotRetryableExceptions(MensajeInvalidoException.class);
        manejador.setRetryListeners((registro, ex, intento) -> log.warn(
                "[Kafka][REINTENTO] Intento {} de {} falló topico={} particion={} offset={} clave={} causa={}",
                intento, props.reintentos().maximo(), registro.topic(), registro.partition(), registro.offset(),
                registro.key(), causa(ex)));
        return manejador;
    }

    /** Mensaje de la causa raíz (el listener envuelve la excepción original). */
    private static String causa(Throwable ex) {
        Throwable raiz = ex;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        return raiz.getClass().getSimpleName() + ": " + raiz.getMessage();
    }
}

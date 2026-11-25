package cl.duoc.gestorcitas.citas.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Configuración del PRODUCTOR Kafka de ms-citas.
 * <p>
 * Spring Boot crea el KafkaTemplate con las propiedades de {@code spring.kafka.producer}
 * (acks=all, idempotencia, clave String). Aquí solo se ajusta el serializador del valor para que
 * use el mismo ObjectMapper de la API: fechas ISO-8601 legibles en Kafka-UI y sin cabeceras de tipo
 * Java, así cada consumidor deserializa el JSON a su propia clase.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public DefaultKafkaProducerFactoryCustomizer serializadorJsonDeEventos(ObjectMapper objectMapper) {
        return factory -> {
            JsonSerializer<Object> serializer = new JsonSerializer<>(objectMapper);
            serializer.setAddTypeInfo(false);
            @SuppressWarnings("unchecked")
            DefaultKafkaProducerFactory<Object, Object> f = (DefaultKafkaProducerFactory<Object, Object>) factory;
            f.setValueSerializer(serializer);
        };
    }
}

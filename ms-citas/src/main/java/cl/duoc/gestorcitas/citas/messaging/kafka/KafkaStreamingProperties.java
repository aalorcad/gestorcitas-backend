package cl.duoc.gestorcitas.citas.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del streaming Kafka de ms-citas, leída desde {@code app.kafka} (application.yml).
 * La conexión al clúster (bootstrap servers, acks, idempotencia) está en {@code spring.kafka}.
 *
 * @param habilitado false desactiva la publicación (tests o entornos sin clúster Kafka)
 * @param topicos    nombres de los tópicos donde publica ms-citas
 */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaStreamingProperties(boolean habilitado, Topicos topicos) {

    /** @param citasEventos tópico con los cambios de estado de las citas (clave = citaId) */
    public record Topicos(String citasEventos) {
    }
}

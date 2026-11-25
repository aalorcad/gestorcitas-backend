package cl.duoc.gestorcitas.usuarios.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del streaming Kafka de ms-usuarios, leída desde {@code app.kafka} (application.yml).
 * La conexión al clúster (bootstrap servers, acks, idempotencia) está en {@code spring.kafka}.
 *
 * @param habilitado false desactiva la publicación (tests o entornos sin clúster Kafka)
 * @param topicos    nombres de los tópicos donde publica ms-usuarios
 */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaStreamingProperties(boolean habilitado, Topicos topicos) {

    /** @param usuariosEstado tópico compactado con el último estado de cada usuario (clave = usuarioId) */
    public record Topicos(String usuariosEstado) {
    }
}

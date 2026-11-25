package cl.duoc.gestorcitas.analitica.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del consumo Kafka de ms-analitica, leída desde {@code app.kafka} (application.yml).
 *
 * @param habilitado false: los listeners no se inician (tests sin clúster)
 * @param grupo      grupo de consumidores (todas las réplicas de ms-analitica comparten el grupo
 *                   y Kafka les reparte las particiones)
 * @param topicos    tópicos que consume
 * @param reintentos política de reintentos antes de enviar el registro al tópico de errores
 * @param sufijoDlt  sufijo del tópico de errores: el registro fallido va a {@code <tópico><sufijoDlt>}, misma partición
 */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaConsumoProperties(boolean habilitado, String grupo, Topicos topicos, Reintentos reintentos,
                                     String sufijoDlt) {

    public record Topicos(String citasEventos, String usuariosEstado) {
    }

    /**
     * @param maximo   intentos TOTALES de procesamiento de un registro (1 original + reintentos)
     * @param esperaMs espera fija entre intentos
     */
    public record Reintentos(int maximo, long esperaMs) {
    }
}

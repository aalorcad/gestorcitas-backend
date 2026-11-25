package cl.duoc.gestorcitas.notificaciones.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Consumo Kafka de ms-notificaciones, leído desde {@code app.kafka} (application.yml).
 *
 * @param habilitado false: el listener no se inicia (tests o entornos sin clúster)
 * @param grupo      grupo de consumidores; es DISTINTO al de ms-analitica, por eso ambos reciben todos los eventos
 * @param topicos    tópicos que consume
 * @param reintentos intentos totales y espera antes de enviar el registro al tópico de errores
 * @param sufijoDlt  sufijo del tópico de errores: el registro fallido va a {@code <tópico><sufijoDlt>}, misma partición
 */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaHistorialProperties(boolean habilitado, String grupo, Topicos topicos, Reintentos reintentos,
                                       String sufijoDlt) {

    public record Topicos(String citasEventos) {
    }

    public record Reintentos(int maximo, long esperaMs) {
    }
}

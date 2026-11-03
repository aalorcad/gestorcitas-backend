package cl.duoc.gestorcitas.rabbitadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Acceso a la API HTTP de management de RabbitMQ ({@code app.rabbitmq.management}). */
@ConfigurationProperties(prefix = "app.rabbitmq.management")
public record RabbitManagementProperties(String url, String username, String password, String vhost) {

    public RabbitManagementProperties {
        if (vhost == null || vhost.isBlank()) {
            vhost = "/";
        }
    }
}

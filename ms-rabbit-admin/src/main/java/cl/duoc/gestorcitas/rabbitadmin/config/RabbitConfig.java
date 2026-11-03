package cl.duoc.gestorcitas.rabbitadmin.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración RabbitMQ del administrador.
 * <ul>
 *   <li>{@link AmqpAdmin}: declara y elimina colas, exchanges y bindings por AMQP.</li>
 *   <li>{@link RabbitTemplate}: publica mensajes de prueba (útil para demostrar el flujo hacia la DLQ).</li>
 * </ul>
 * Este servicio NO declara topología propia: solo administra la que se le pide por la API.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        // Los errores de declaración se propagan a la API (no se ignoran) para informar al usuario
        admin.setIgnoreDeclarationExceptions(false);
        return admin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }
}

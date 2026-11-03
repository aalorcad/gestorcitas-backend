package cl.duoc.gestorcitas.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Microservicio CONSUMIDOR de RabbitMQ.
 * Procesa de forma asíncrona las notificaciones de citas, la generación de comprobantes
 * y la auditoría de usuarios, sin acoplarse a ms-citas ni a ms-usuarios.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsNotificacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsNotificacionesApplication.class, args);
    }
}

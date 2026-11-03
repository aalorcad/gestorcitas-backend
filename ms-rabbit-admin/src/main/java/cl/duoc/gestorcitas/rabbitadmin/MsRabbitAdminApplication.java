package cl.duoc.gestorcitas.rabbitadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Microservicio ADMINISTRADOR de RabbitMQ: expone una API REST para crear y eliminar
 * colas, exchanges y bindings. Es interno: el BFF lo publica bajo /api/admin/mensajeria (rol Admin).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsRabbitAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsRabbitAdminApplication.class, args);
    }
}

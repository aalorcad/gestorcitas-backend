package cl.duoc.gestorcitas.kafkaadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ms-kafka-admin: API REST para administrar el clúster Kafka con AdminClient
 * (clúster, tópicos y grupos de consumidores). El BFF la publica en {@code /api/admin/kafka/**} solo para Admin.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsKafkaAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsKafkaAdminApplication.class, args);
    }
}

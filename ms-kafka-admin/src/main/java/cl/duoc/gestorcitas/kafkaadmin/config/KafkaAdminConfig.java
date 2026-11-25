package cl.duoc.gestorcitas.kafkaadmin.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente de administración del clúster.
 * <ul>
 *   <li>{@link AdminClient}: lo usa la API REST (tópicos, grupos, offsets). Usa la misma conexión que
 *       Spring Boot configura para {@link KafkaAdmin} ({@code spring.kafka.bootstrap-servers}).</li>
 *   <li>{@link KafkaAdmin.NewTopics}: declara los tópicos del sistema definidos en {@code app.admin.topicos-sistema}.
 *       Al arrancar, KafkaAdmin crea los que falten (mismo diseño que scripts/topicos.sh del repo gestorcitas-kafka).
 *       Ningún nombre, partición ni retención está escrito en el código.</li>
 * </ul>
 */
@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        Map<String, Object> config = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        config.putIfAbsent(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);
        config.putIfAbsent(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 15_000);
        return AdminClient.create(config);
    }

    @Bean
    public KafkaAdmin.NewTopics topicosDelSistema(KafkaAdminProperties props) {
        NewTopic[] topicos = props.topicosSistema().stream()
                .map(t -> TopicBuilder.name(t.nombre())
                        .partitions(t.particiones())
                        .replicas(t.replicas())
                        .configs(t.configuracion() == null ? Map.of() : t.configuracion())
                        .build())
                .toArray(NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topicos);
    }
}

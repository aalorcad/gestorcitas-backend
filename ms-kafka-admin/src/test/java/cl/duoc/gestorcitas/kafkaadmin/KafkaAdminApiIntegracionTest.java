package cl.duoc.gestorcitas.kafkaadmin;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * La API completa contra un broker Kafka embebido (1 nodo): códigos HTTP 200/201/204/400/404/409
 * para tópicos y grupos de consumidores, incluido el cálculo del lag y el reinicio de offsets.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@AutoConfigureMockMvc
@EmbeddedKafka(count = 1)
@DirtiesContext
class KafkaAdminApiIntegracionTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void clusterMuestraLosBrokers() throws Exception {
        mvc.perform(get("/cluster"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBrokers").value(1))
                .andExpect(jsonPath("$.clusterId").isNotEmpty());
    }

    @Test
    void cicloDeVidaDeUnTopico() throws Exception {
        mvc.perform(json(post("/topics"), """
                        {"nombre":"demo.ciclo","particiones":2,"factorReplicacion":1,
                         "configuracion":{"retention.ms":"60000"}}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/topics/demo.ciclo"))
                .andExpect(jsonPath("$.particiones.length()").value(2))
                .andExpect(jsonPath("$.configuracion['retention.ms']").value("60000"))
                .andExpect(jsonPath("$.protegido").value(false));

        // ya existe -> 409
        mvc.perform(json(post("/topics"), """
                        {"nombre":"demo.ciclo","particiones":1,"factorReplicacion":1}"""))
                .andExpect(status().isConflict());

        mvc.perform(get("/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("demo.ciclo")));

        mvc.perform(json(patch("/topics/demo.ciclo/config"), """
                        {"configuracion":{"retention.ms":"120000","cleanup.policy":"compact"}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuracion['retention.ms']").value("120000"))
                .andExpect(jsonPath("$.configuracion['cleanup.policy']").value("compact"));

        // Kafka no permite reducir particiones -> 400; aumentar -> 200
        mvc.perform(json(post("/topics/demo.ciclo/partitions"), "{\"total\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/topics/demo.ciclo/partitions"), "{\"total\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.particiones.length()").value(4));

        mvc.perform(delete("/topics/demo.ciclo")).andExpect(status().isNoContent());
    }

    @Test
    void erroresDeEntradaYRecursosInexistentes() throws Exception {
        // replicación mayor que la cantidad de brokers -> Kafka la rechaza -> 400
        mvc.perform(json(post("/topics"), """
                        {"nombre":"demo.rf","particiones":1,"factorReplicacion":3}"""))
                .andExpect(status().isBadRequest());
        // configuración fuera de la lista permitida -> 400
        mvc.perform(json(post("/topics"), """
                        {"nombre":"demo.cfg","particiones":1,"factorReplicacion":1,
                         "configuracion":{"compression.type":"gzip"}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("compression.type")));
        // Bean Validation -> 400 con detalle por campo
        mvc.perform(json(post("/topics"), """
                        {"nombre":"con espacio","particiones":0,"factorReplicacion":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(2));

        mvc.perform(get("/topics/no.existe")).andExpect(status().isNotFound());
        mvc.perform(get("/consumer-groups/grupo-inexistente")).andExpect(status().isNotFound());

        // tópicos del sistema protegidos -> 409
        mvc.perform(delete("/topics/gestorcitas.citas.eventos")).andExpect(status().isConflict());
        mvc.perform(json(post("/topics/gestorcitas.citas.eventos/partitions"), "{\"total\":12}"))
                .andExpect(status().isConflict());
    }

    @Test
    void lagYReinicioDeOffsetsDeUnGrupo() throws Exception {
        mvc.perform(json(post("/topics"), """
                        {"nombre":"demo.lag","particiones":1,"factorReplicacion":1}"""))
                .andExpect(status().isCreated());

        KafkaTemplate<String, String> productor = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(broker), new StringSerializer(), new StringSerializer()));
        for (int i = 0; i < 3; i++) {
            productor.send("demo.lag", "k", "v" + i).get();
        }
        productor.destroy();

        // el grupo procesó solo el primer registro (offset confirmado = 1) y luego se detuvo
        Map<String, Object> props = KafkaTestUtils.consumerProps("grupo-demo", "false", broker);
        try (Consumer<String, String> consumidor = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumidor.commitSync(Map.of(new TopicPartition("demo.lag", 0), new OffsetAndMetadata(1)));
        }

        mvc.perform(get("/consumer-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].grupo", hasItem("grupo-demo")));
        mvc.perform(get("/consumer-groups/grupo-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EMPTY"))
                .andExpect(jsonPath("$.lag[0].offsetConfirmado").value(1))
                .andExpect(jsonPath("$.lag[0].offsetFinal").value(3))
                .andExpect(jsonPath("$.lagTotal").value(2));

        mvc.perform(json(post("/consumer-groups/grupo-demo/reset-offsets"), """
                        {"topico":"demo.lag","estrategia":"EARLIEST"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nuevosOffsets['0']").value(0));
        mvc.perform(get("/consumer-groups/grupo-demo"))
                .andExpect(jsonPath("$.lagTotal").value(3));

        mvc.perform(json(post("/consumer-groups/grupo-demo/reset-offsets"), """
                        {"topico":"demo.lag","estrategia":"OFFSET","offset":99}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nuevosOffsets['0']").value(3));   // se ajusta al final

        mvc.perform(delete("/consumer-groups/grupo-demo")).andExpect(status().isNoContent());
        mvc.perform(get("/consumer-groups/grupo-demo")).andExpect(status().isNotFound());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String cuerpo) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }
}

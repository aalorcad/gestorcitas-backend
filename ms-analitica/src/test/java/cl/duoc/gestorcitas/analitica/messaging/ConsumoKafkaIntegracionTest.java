package cl.duoc.gestorcitas.analitica.messaging;

import cl.duoc.gestorcitas.analitica.entity.UsuarioEstado;
import cl.duoc.gestorcitas.analitica.repository.EventoCitaRepository;
import cl.duoc.gestorcitas.analitica.repository.UsuarioEstadoRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de punta a punta del consumidor con un broker Kafka embebido:
 * consumo y persistencia, descarte de duplicados, actualización por tópico compactado y envío a .DLT
 * de registros inválidos (contrato incumplido) y corruptos (JSON ilegible).
 */
@SpringBootTest(properties = {
        "app.kafka.habilitado=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = {
        ConsumoKafkaIntegracionTest.CITAS,
        ConsumoKafkaIntegracionTest.CITAS_DLT,
        ConsumoKafkaIntegracionTest.USUARIOS,
        ConsumoKafkaIntegracionTest.USUARIOS_DLT
})
@DirtiesContext
class ConsumoKafkaIntegracionTest {

    static final String CITAS = "gestorcitas.citas.eventos";
    static final String CITAS_DLT = "gestorcitas.citas.eventos.DLT";
    static final String USUARIOS = "gestorcitas.usuarios.estado";
    static final String USUARIOS_DLT = "gestorcitas.usuarios.estado.DLT";

    private static final Duration ESPERA = Duration.ofSeconds(30);

    @Autowired
    private EmbeddedKafkaBroker broker;
    @Autowired
    private EventoCitaRepository eventos;
    @Autowired
    private UsuarioEstadoRepository usuarios;

    private KafkaTemplate<String, String> productor;
    private Consumer<String, String> lectorDlt;

    @BeforeEach
    void setUp() {
        productor = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(broker), new StringSerializer(), new StringSerializer()));
        Map<String, Object> props = KafkaTestUtils.consumerProps("lector-dlt-prueba", "false", broker);
        props.put("auto.offset.reset", "earliest");
        lectorDlt = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        broker.consumeFromEmbeddedTopics(lectorDlt, CITAS_DLT);
    }

    @AfterEach
    void tearDown() {
        lectorDlt.close();
        productor.destroy();
    }

    @Test
    void procesaEventosValidosYEnviaLosInvalidosALaDlt() throws Exception {
        String valido = """
                {"eventoId":"ev-1","tipo":"CREADA","citaId":42,"pacienteId":"oid","medicoId":3,
                 "medicoNombre":"Dra. Rojas","especialidadId":10,"especialidadNombre":"Medicina General",
                 "fechaHora":"2026-09-29T10:00:00","estado":"PENDIENTE","ocurridoEn":"2026-09-28T09:00:00"}""";
        String sinCitaId = """
                {"eventoId":"ev-2","tipo":"CREADA","ocurridoEn":"2026-09-28T09:00:00"}""";
        String corrupto = "{esto no es json";

        productor.send(CITAS, "42", valido).get();
        productor.send(CITAS, "42", valido).get();      // entrega repetida: no debe contarse dos veces
        productor.send(CITAS, "43", sinCitaId).get();
        productor.send(CITAS, "44", corrupto).get();

        esperar(() -> eventos.existsByEventoId("ev-1"));
        List<ConsumerRecord<String, String>> enDlt = leerDlt(2);

        assertThat(eventos.count()).isEqualTo(1);
        assertThat(enDlt).extracting(ConsumerRecord::key).containsExactlyInAnyOrder("43", "44");
        ConsumerRecord<String, String> invalido = enDlt.stream().filter(r -> "43".equals(r.key())).findFirst().orElseThrow();
        assertThat(invalido.value()).contains("\"eventoId\":\"ev-2\"");
        assertThat(cabecera(invalido, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(CITAS);
        // la traza guardada por DeadLetterPublishingRecoverer conserva la causa original
        assertThat(cabecera(invalido, KafkaHeaders.DLT_EXCEPTION_STACKTRACE))
                .contains("EventoInvalidoException").contains("citaId");
        ConsumerRecord<String, String> ilegible = enDlt.stream().filter(r -> "44".equals(r.key())).findFirst().orElseThrow();
        assertThat(ilegible.value()).isEqualTo(corrupto);
    }

    @Test
    void elTopicoCompactadoMantieneElUltimoEstadoDelUsuario() throws Exception {
        productor.send(USUARIOS, "7", """
                {"eventoId":"u-1","tipo":"REGISTRADO","usuarioId":7,"email":"ana@demo.cl","nombre":"Ana",
                 "roles":["Paciente"],"activo":true,"actualizadoEn":"2026-09-28T09:00:00"}""").get();
        productor.send(USUARIOS, "7", """
                {"eventoId":"u-2","tipo":"DESACTIVADO","usuarioId":7,"email":"ana@demo.cl","nombre":"Ana",
                 "roles":["Paciente"],"activo":false,"actualizadoEn":"2026-09-28T09:05:00"}""").get();

        esperar(() -> usuarios.findById(7L).map(u -> "u-2".equals(u.getUltimoEventoId())).orElse(false));

        Optional<UsuarioEstado> estado = usuarios.findById(7L);
        assertThat(estado).isPresent();
        assertThat(estado.get().isActivo()).isFalse();
        assertThat(estado.get().getUltimoTipo()).isEqualTo("DESACTIVADO");

        productor.send(USUARIOS, "7", null).get();   // tombstone
        esperar(() -> usuarios.findById(7L).isEmpty());
    }

    private List<ConsumerRecord<String, String>> leerDlt(int esperados) {
        List<ConsumerRecord<String, String>> leidos = new ArrayList<>();
        long limite = System.currentTimeMillis() + ESPERA.toMillis();
        while (leidos.size() < esperados && System.currentTimeMillis() < limite) {
            KafkaTestUtils.getRecords(lectorDlt, Duration.ofSeconds(2)).forEach(leidos::add);
        }
        return leidos;
    }

    private static String cabecera(ConsumerRecord<String, String> registro, String nombre) {
        var h = registro.headers().lastHeader(nombre);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    private static void esperar(BooleanSupplier condicion) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA.toMillis();
        while (!condicion.getAsBoolean()) {
            if (System.currentTimeMillis() > limite) {
                throw new AssertionError("La condición no se cumplió en " + ESPERA.toSeconds() + " s");
            }
            Thread.sleep(200);
        }
    }
}

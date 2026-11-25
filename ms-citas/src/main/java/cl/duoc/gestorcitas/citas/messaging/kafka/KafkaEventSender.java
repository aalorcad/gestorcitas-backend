package cl.duoc.gestorcitas.citas.messaging.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;

/**
 * Envío técnico de registros a Kafka.
 * <ul>
 *   <li>Si hay una transacción activa, el envío se difiere hasta DESPUÉS del commit: nunca se publica
 *       un cambio que finalmente no se guardó.</li>
 *   <li>El envío es asíncrono: el resultado (partición y offset asignados, o el error) se registra en el
 *       log cuando el clúster responde. Los errores NO se propagan: la cita ya está guardada en Oracle.</li>
 *   <li>Cada registro lleva la cabecera {@code eventoId} para trazabilidad en Kafka-UI y en los consumidores.</li>
 * </ul>
 */
@Component
public class KafkaEventSender {

    static final String CABECERA_EVENTO_ID = "eventoId";

    private static final Logger log = LoggerFactory.getLogger(KafkaEventSender.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void enviarTrasCommit(String topico, String clave, Object payload, String eventoId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(topico, clave, payload, eventoId);
                }
            });
        } else {
            enviar(topico, clave, payload, eventoId);
        }
    }

    void enviar(String topico, String clave, Object payload, String eventoId) {
        ProducerRecord<String, Object> registro = new ProducerRecord<>(topico, clave, payload);
        registro.headers().add(CABECERA_EVENTO_ID, eventoId.getBytes(StandardCharsets.UTF_8));
        try {
            kafkaTemplate.send(registro).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("[Kafka] No se pudo publicar topico={} clave={} eventoId={}. "
                            + "La operación de negocio se mantiene. Causa: {}", topico, clave, eventoId, error.getMessage());
                } else {
                    RecordMetadata meta = resultado.getRecordMetadata();
                    log.info("[Kafka] Publicado topico={} particion={} offset={} clave={} eventoId={}",
                            meta.topic(), meta.partition(), meta.offset(), clave, eventoId);
                }
            });
        } catch (RuntimeException ex) {
            // p. ej. clúster inalcanzable al pedir metadatos (max.block.ms)
            log.error("[Kafka] No se pudo publicar topico={} clave={} eventoId={}. "
                    + "La operación de negocio se mantiene. Causa: {}", topico, clave, eventoId, ex.getMessage());
        }
    }
}

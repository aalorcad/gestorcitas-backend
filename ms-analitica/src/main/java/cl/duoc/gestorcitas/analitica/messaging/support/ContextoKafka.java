package cl.duoc.gestorcitas.analitica.messaging.support;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/** Texto con la ubicación exacta de un registro en Kafka, para que cada línea de log sea rastreable. */
public final class ContextoKafka {

    private ContextoKafka() {
    }

    public static String de(ConsumerRecord<?, ?> registro) {
        return "topico=" + registro.topic()
                + " particion=" + registro.partition()
                + " offset=" + registro.offset()
                + " clave=" + registro.key();
    }
}

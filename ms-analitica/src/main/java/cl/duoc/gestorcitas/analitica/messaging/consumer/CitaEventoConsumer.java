package cl.duoc.gestorcitas.analitica.messaging.consumer;

import cl.duoc.gestorcitas.analitica.messaging.event.CitaEventoMsg;
import cl.duoc.gestorcitas.analitica.messaging.support.ContextoKafka;
import cl.duoc.gestorcitas.analitica.messaging.support.ValidadorDeEventos;
import cl.duoc.gestorcitas.analitica.service.AnaliticaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del tópico de eventos de citas (grupo "analitica").
 * <p>
 * El JSON se convierte a {@link CitaEventoMsg} con ErrorHandlingDeserializer: un registro corrupto no
 * detiene el consumo, va directo al tópico .DLT. Un evento que no cumple el contrato tampoco se reintenta.
 * Un error transitorio (p. ej. Oracle no responde) se reintenta y, si persiste, también termina en .DLT.
 * El offset se confirma registro a registro (ack-mode RECORD) solo cuando el procesamiento terminó.
 */
@Component
public class CitaEventoConsumer {

    private static final Logger log = LoggerFactory.getLogger(CitaEventoConsumer.class);

    private final AnaliticaService servicio;
    private final ValidadorDeEventos validador;

    public CitaEventoConsumer(AnaliticaService servicio, ValidadorDeEventos validador) {
        this.servicio = servicio;
        this.validador = validador;
    }

    @KafkaListener(
            id = "analitica-citas-eventos",
            groupId = "${app.kafka.grupo}",
            autoStartup = "${app.kafka.habilitado:true}",
            topics = "${app.kafka.topicos.citas-eventos}",
            properties = "spring.json.value.default.type=cl.duoc.gestorcitas.analitica.messaging.event.CitaEventoMsg")
    public void consumir(ConsumerRecord<String, CitaEventoMsg> registro) {
        String contexto = ContextoKafka.de(registro);
        CitaEventoMsg evento = registro.value();
        validador.validar(evento, contexto);

        AnaliticaService.Resultado resultado = servicio.registrarEventoCita(evento, registro.partition(), registro.offset());
        if (resultado == AnaliticaService.Resultado.DUPLICADO) {
            log.warn("[Kafka][analitica][DUPLICADO] Evento {} de la cita {} ya procesado; se ignora {}",
                    evento.eventoId(), evento.citaId(), contexto);
        } else {
            log.info("[Kafka][analitica] Evento {} de la cita {} registrado (eventoId={}) {}",
                    evento.tipo(), evento.citaId(), evento.eventoId(), contexto);
        }
    }
}

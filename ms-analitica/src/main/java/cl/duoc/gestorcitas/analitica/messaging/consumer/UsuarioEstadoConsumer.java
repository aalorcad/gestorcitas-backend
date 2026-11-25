package cl.duoc.gestorcitas.analitica.messaging.consumer;

import cl.duoc.gestorcitas.analitica.exception.EventoInvalidoException;
import cl.duoc.gestorcitas.analitica.messaging.event.UsuarioEstadoMsg;
import cl.duoc.gestorcitas.analitica.messaging.support.ContextoKafka;
import cl.duoc.gestorcitas.analitica.messaging.support.ValidadorDeEventos;
import cl.duoc.gestorcitas.analitica.service.AnaliticaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del tópico COMPACTADO de estado de usuarios (grupo "analitica").
 * <p>
 * Cada registro trae el estado completo del usuario y reemplaza al anterior (misma clave = usuarioId).
 * Un registro con valor nulo es un <i>tombstone</i>: indica que la clave se eliminó del tópico.
 */
@Component
public class UsuarioEstadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(UsuarioEstadoConsumer.class);

    private final AnaliticaService servicio;
    private final ValidadorDeEventos validador;

    public UsuarioEstadoConsumer(AnaliticaService servicio, ValidadorDeEventos validador) {
        this.servicio = servicio;
        this.validador = validador;
    }

    @KafkaListener(
            id = "analitica-usuarios-estado",
            groupId = "${app.kafka.grupo}",
            autoStartup = "${app.kafka.habilitado:true}",
            topics = "${app.kafka.topicos.usuarios-estado}",
            properties = "spring.json.value.default.type=cl.duoc.gestorcitas.analitica.messaging.event.UsuarioEstadoMsg")
    public void consumir(ConsumerRecord<String, UsuarioEstadoMsg> registro) {
        String contexto = ContextoKafka.de(registro);

        if (registro.value() == null) {
            AnaliticaService.Resultado resultado = servicio.eliminarUsuario(claveComoId(registro.key(), contexto));
            log.info("[Kafka][analitica] Tombstone del usuario {}: {} {}", registro.key(), resultado, contexto);
            return;
        }

        UsuarioEstadoMsg estado = registro.value();
        validador.validar(estado, contexto);
        AnaliticaService.Resultado resultado = servicio.aplicarEstadoUsuario(estado);
        switch (resultado) {
            case DUPLICADO -> log.warn("[Kafka][analitica][DUPLICADO] Estado {} del usuario {} ya aplicado {}",
                    estado.eventoId(), estado.usuarioId(), contexto);
            case IGNORADO_ANTIGUO -> log.warn("[Kafka][analitica] Estado del usuario {} más antiguo que el guardado; se ignora {}",
                    estado.usuarioId(), contexto);
            default -> log.info("[Kafka][analitica] Usuario {} {} (tipo={}, activo={}) {}",
                    estado.usuarioId(), resultado, estado.tipo(), estado.activo(), contexto);
        }
    }

    private static Long claveComoId(String clave, String contexto) {
        try {
            return Long.valueOf(clave);
        } catch (NumberFormatException | NullPointerException ex) {
            throw new EventoInvalidoException("Tombstone con clave no numérica " + contexto);
        }
    }
}

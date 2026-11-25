package cl.duoc.gestorcitas.usuarios.messaging.kafka;

import cl.duoc.gestorcitas.usuarios.entity.Usuario;
import cl.duoc.gestorcitas.usuarios.messaging.TipoEventoUsuario;
import cl.duoc.gestorcitas.usuarios.messaging.UsuarioStreamPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Productor Kafka de ms-usuarios: publica el estado del usuario en
 * {@code app.kafka.topicos.usuarios-estado} con clave {@code usuarioId} (tópico compactado).
 */
@Component
public class KafkaUsuarioStreamPublisher implements UsuarioStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaUsuarioStreamPublisher.class);

    private final KafkaEventSender sender;
    private final KafkaStreamingProperties props;
    private final Clock clock;

    public KafkaUsuarioStreamPublisher(KafkaEventSender sender, KafkaStreamingProperties props, Clock clock) {
        this.sender = sender;
        this.props = props;
        this.clock = clock;
    }

    @Override
    public void publicar(Usuario usuario, TipoEventoUsuario tipo) {
        if (!props.habilitado()) {
            log.debug("Kafka deshabilitado: no se publica estado {} del usuario {}", tipo, usuario.getId());
            return;
        }
        UsuarioEstadoKafka estado = UsuarioEstadoKafka.desde(usuario, tipo, LocalDateTime.now(clock));
        sender.enviarTrasCommit(
                props.topicos().usuariosEstado(),
                String.valueOf(usuario.getId()),
                estado,
                estado.eventoId());
    }
}

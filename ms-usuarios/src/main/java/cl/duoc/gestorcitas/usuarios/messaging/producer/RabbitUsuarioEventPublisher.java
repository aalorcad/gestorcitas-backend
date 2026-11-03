package cl.duoc.gestorcitas.usuarios.messaging.producer;

import cl.duoc.gestorcitas.usuarios.entity.Usuario;
import cl.duoc.gestorcitas.usuarios.messaging.TipoEventoUsuario;
import cl.duoc.gestorcitas.usuarios.messaging.UsuarioEventPublisher;
import cl.duoc.gestorcitas.usuarios.messaging.config.MessagingProperties;
import cl.duoc.gestorcitas.usuarios.messaging.event.UsuarioEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/** Productor RabbitMQ de ms-usuarios: publica los eventos de auditoría de usuarios. */
@Component
public class RabbitUsuarioEventPublisher implements UsuarioEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitUsuarioEventPublisher.class);

    private final RabbitEventSender sender;
    private final MessagingProperties props;
    private final Clock clock;

    public RabbitUsuarioEventPublisher(RabbitEventSender sender, MessagingProperties props, Clock clock) {
        this.sender = sender;
        this.props = props;
        this.clock = clock;
    }

    @Override
    public void publicar(Usuario usuario, TipoEventoUsuario tipo, String actorId) {
        if (!props.habilitado()) {
            log.debug("Mensajería deshabilitada: no se publica evento {} del usuario {}", tipo, usuario.getId());
            return;
        }
        UsuarioEvento evento = UsuarioEvento.desde(usuario, tipo, actorId, LocalDateTime.now(clock));
        sender.enviarTrasCommit(
                props.exchanges().usuariosEventos(),
                props.routingKeys().paraEvento(tipo),
                evento,
                evento.eventoId());
    }
}

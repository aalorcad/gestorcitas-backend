package cl.duoc.gestorcitas.notificaciones.messaging.consumer.usuarios;

import cl.duoc.gestorcitas.notificaciones.messaging.config.MessagingProperties;
import cl.duoc.gestorcitas.notificaciones.messaging.event.UsuarioEvento;
import cl.duoc.gestorcitas.notificaciones.messaging.support.ProcesadorDeMensajes;
import cl.duoc.gestorcitas.notificaciones.service.AuditoriaService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dominio USUARIOS - consumidor de los eventos de usuarios para auditoría.
 * Cola: {@code app.messaging.auditoria-usuarios.cola}. ACK manual a través de {@link ProcesadorDeMensajes}.
 */
@Component
public class AuditoriaUsuarioConsumer {

    private final ProcesadorDeMensajes procesador;
    private final AuditoriaService auditoria;
    private final MessagingProperties props;

    public AuditoriaUsuarioConsumer(ProcesadorDeMensajes procesador, AuditoriaService auditoria,
                                    MessagingProperties props) {
        this.procesador = procesador;
        this.auditoria = auditoria;
        this.props = props;
    }

    @RabbitListener(id = "auditoria-usuarios", queues = "${app.messaging.auditoria-usuarios.cola}")
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar(message, channel, props.auditoriaUsuarios(), UsuarioEvento.class, auditoria::registrar);
    }
}

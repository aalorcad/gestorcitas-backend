package cl.duoc.gestorcitas.notificaciones.messaging.consumer.citas;

import cl.duoc.gestorcitas.notificaciones.messaging.config.MessagingProperties;
import cl.duoc.gestorcitas.notificaciones.messaging.event.CitaEvento;
import cl.duoc.gestorcitas.notificaciones.messaging.support.ProcesadorDeMensajes;
import cl.duoc.gestorcitas.notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dominio CITAS - consumidor de los eventos de citas (creada, confirmada, cancelada, atendida).
 * Cola: {@code app.messaging.notificaciones-citas.cola}. ACK manual a través de {@link ProcesadorDeMensajes}.
 */
@Component
public class NotificacionCitaConsumer {

    private final ProcesadorDeMensajes procesador;
    private final NotificacionService notificaciones;
    private final MessagingProperties props;

    public NotificacionCitaConsumer(ProcesadorDeMensajes procesador, NotificacionService notificaciones,
                                    MessagingProperties props) {
        this.procesador = procesador;
        this.notificaciones = notificaciones;
        this.props = props;
    }

    @RabbitListener(id = "notificaciones-citas", queues = "${app.messaging.notificaciones-citas.cola}")
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar(message, channel, props.notificacionesCitas(), CitaEvento.class,
                notificaciones::notificarCambioDeCita);
    }
}

package cl.duoc.gestorcitas.notificaciones.messaging.consumer.citas;

import cl.duoc.gestorcitas.notificaciones.messaging.config.MessagingProperties;
import cl.duoc.gestorcitas.notificaciones.messaging.event.SolicitudComprobante;
import cl.duoc.gestorcitas.notificaciones.messaging.support.ProcesadorDeMensajes;
import cl.duoc.gestorcitas.notificaciones.service.ComprobanteService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dominio CITAS - consumidor de solicitudes de comprobante (exchange direct).
 * Cola: {@code app.messaging.comprobantes-citas.cola}. ACK manual a través de {@link ProcesadorDeMensajes}.
 */
@Component
public class ComprobanteCitaConsumer {

    private final ProcesadorDeMensajes procesador;
    private final ComprobanteService comprobantes;
    private final MessagingProperties props;

    public ComprobanteCitaConsumer(ProcesadorDeMensajes procesador, ComprobanteService comprobantes,
                                   MessagingProperties props) {
        this.procesador = procesador;
        this.comprobantes = comprobantes;
        this.props = props;
    }

    @RabbitListener(id = "comprobantes-citas", queues = "${app.messaging.comprobantes-citas.cola}")
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar(message, channel, props.comprobantesCitas(), SolicitudComprobante.class,
                comprobantes::generar);
    }
}

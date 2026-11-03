package cl.duoc.gestorcitas.notificaciones.messaging.config;

import cl.duoc.gestorcitas.notificaciones.messaging.config.MessagingProperties.Flujo;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología RabbitMQ que consume ms-notificaciones. RabbitAdmin declara estos beans al conectarse.
 *
 * <pre>
 * Flujo                  Exchange (tipo)                    Routing key           Cola principal            DLQ
 * ---------------------  ---------------------------------  --------------------  ------------------------  ----------------------------
 * Notificación de citas  gestorcitas.citas.eventos (topic)   cita.*               q.notificaciones.citas    q.notificaciones.citas.dlq
 * Comprobante de cita    gestorcitas.citas.comprobantes (dir) comprobante.generar q.comprobantes.citas      q.comprobantes.citas.dlq
 * Auditoría de usuarios  gestorcitas.usuarios.eventos (topic) usuario.#           q.auditoria.usuarios      q.auditoria.usuarios.dlq
 * </pre>
 *
 * Cada cola principal tiene:
 * <ul>
 *   <li>{@code x-dead-letter-exchange} = gestorcitas.dlx y {@code x-dead-letter-routing-key} = su DLQ:
 *       cuando el consumidor hace {@code basicNack(requeue=false)} el broker mueve el mensaje a la DLQ.</li>
 *   <li>Una cola de reintento con TTL cuyo dead-letter apunta de vuelta a la cola principal
 *       (vía exchange por defecto): así un error temporal se reintenta con espera, sin bucles inmediatos.</li>
 * </ul>
 * Todos los nombres provienen de {@link MessagingProperties}.
 */
@Configuration
public class RabbitConfig {

    /** Nombre del exchange por defecto de RabbitMQ: enruta directo a la cola cuyo nombre es la routing key. */
    private static final String EXCHANGE_POR_DEFECTO = "";

    private final MessagingProperties props;

    public RabbitConfig(MessagingProperties props) {
        this.props = props;
    }

    // =================================================================== Dead letter exchange (común)

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.deadLetterExchange(), true, false);
    }

    // =================================================================== 1. Notificaciones de citas (TOPIC)

    @Bean
    public TopicExchange citasEventosExchange() {
        return new TopicExchange(props.notificacionesCitas().exchange(), true, false);
    }

    @Bean
    public Queue notificacionesCitasQueue() {
        return colaPrincipal(props.notificacionesCitas());
    }

    @Bean
    public Queue notificacionesCitasRetryQueue() {
        return colaReintento(props.notificacionesCitas());
    }

    @Bean
    public Queue notificacionesCitasDlq() {
        return QueueBuilder.durable(props.notificacionesCitas().dlq()).build();
    }

    @Bean
    public Binding notificacionesCitasBinding() {
        return BindingBuilder.bind(notificacionesCitasQueue())
                .to(citasEventosExchange())
                .with(props.notificacionesCitas().routingKey());
    }

    @Bean
    public Binding notificacionesCitasDlqBinding() {
        return BindingBuilder.bind(notificacionesCitasDlq())
                .to(deadLetterExchange())
                .with(props.notificacionesCitas().dlq());
    }

    // =================================================================== 2. Comprobantes de citas (DIRECT)

    @Bean
    public DirectExchange citasComprobantesExchange() {
        return new DirectExchange(props.comprobantesCitas().exchange(), true, false);
    }

    @Bean
    public Queue comprobantesCitasQueue() {
        return colaPrincipal(props.comprobantesCitas());
    }

    @Bean
    public Queue comprobantesCitasRetryQueue() {
        return colaReintento(props.comprobantesCitas());
    }

    @Bean
    public Queue comprobantesCitasDlq() {
        return QueueBuilder.durable(props.comprobantesCitas().dlq()).build();
    }

    @Bean
    public Binding comprobantesCitasBinding() {
        return BindingBuilder.bind(comprobantesCitasQueue())
                .to(citasComprobantesExchange())
                .with(props.comprobantesCitas().routingKey());
    }

    @Bean
    public Binding comprobantesCitasDlqBinding() {
        return BindingBuilder.bind(comprobantesCitasDlq())
                .to(deadLetterExchange())
                .with(props.comprobantesCitas().dlq());
    }

    // =================================================================== 3. Auditoría de usuarios (TOPIC)

    @Bean
    public TopicExchange usuariosEventosExchange() {
        return new TopicExchange(props.auditoriaUsuarios().exchange(), true, false);
    }

    @Bean
    public Queue auditoriaUsuariosQueue() {
        return colaPrincipal(props.auditoriaUsuarios());
    }

    @Bean
    public Queue auditoriaUsuariosRetryQueue() {
        return colaReintento(props.auditoriaUsuarios());
    }

    @Bean
    public Queue auditoriaUsuariosDlq() {
        return QueueBuilder.durable(props.auditoriaUsuarios().dlq()).build();
    }

    @Bean
    public Binding auditoriaUsuariosBinding() {
        return BindingBuilder.bind(auditoriaUsuariosQueue())
                .to(usuariosEventosExchange())
                .with(props.auditoriaUsuarios().routingKey());
    }

    @Bean
    public Binding auditoriaUsuariosDlqBinding() {
        return BindingBuilder.bind(auditoriaUsuariosDlq())
                .to(deadLetterExchange())
                .with(props.auditoriaUsuarios().dlq());
    }

    // =================================================================== Constructores de colas

    /** Cola principal: los mensajes rechazados (NACK sin requeue) se envían a la DLQ del flujo. */
    private Queue colaPrincipal(Flujo flujo) {
        return QueueBuilder.durable(flujo.cola())
                .deadLetterExchange(props.deadLetterExchange())
                .deadLetterRoutingKey(flujo.dlq())
                .build();
    }

    /** Cola de reintento: retiene el mensaje {@code esperaMs} y luego lo devuelve a la cola principal. */
    private Queue colaReintento(Flujo flujo) {
        return QueueBuilder.durable(flujo.colaReintento())
                .ttl(props.reintentos().esperaMs())
                .deadLetterExchange(EXCHANGE_POR_DEFECTO)
                .deadLetterRoutingKey(flujo.cola())
                .build();
    }
}

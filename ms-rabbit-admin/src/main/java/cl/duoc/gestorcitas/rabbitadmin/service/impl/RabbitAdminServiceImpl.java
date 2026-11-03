package cl.duoc.gestorcitas.rabbitadmin.service.impl;

import cl.duoc.gestorcitas.rabbitadmin.client.RabbitManagementClient;
import cl.duoc.gestorcitas.rabbitadmin.client.dto.ManagementBinding;
import cl.duoc.gestorcitas.rabbitadmin.client.dto.ManagementExchange;
import cl.duoc.gestorcitas.rabbitadmin.config.AdminProperties;
import cl.duoc.gestorcitas.rabbitadmin.dto.*;
import cl.duoc.gestorcitas.rabbitadmin.exception.ConflictoException;
import cl.duoc.gestorcitas.rabbitadmin.exception.RecursoNoEncontradoException;
import cl.duoc.gestorcitas.rabbitadmin.exception.ServicioNoDisponibleException;
import cl.duoc.gestorcitas.rabbitadmin.exception.SolicitudInvalidaException;
import cl.duoc.gestorcitas.rabbitadmin.service.RabbitAdminService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Administración de RabbitMQ encapsulada en un único servicio.
 * <ul>
 *   <li>Crear / eliminar: por AMQP con {@link AmqpAdmin}.</li>
 *   <li>Listar / verificar existencia de exchanges y bindings: API de management ({@link RabbitManagementClient}).</li>
 *   <li>Reglas: no se duplica, no se elimina lo inexistente, no se tocan los recursos protegidos del sistema.</li>
 * </ul>
 */
@Service
public class RabbitAdminServiceImpl implements RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminServiceImpl.class);

    private final AmqpAdmin amqpAdmin;
    private final RabbitManagementClient management;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final AdminProperties admin;

    public RabbitAdminServiceImpl(AmqpAdmin amqpAdmin, RabbitManagementClient management,
                                  RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, AdminProperties admin) {
        this.amqpAdmin = amqpAdmin;
        this.management = management;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.admin = admin;
    }

    // ================================================================== Colas

    @Override
    public List<ColaResponse> listarColas() {
        return management.listarColas().stream()
                .map(q -> new ColaResponse(q.name(), q.messages(), q.consumers(), q.durable(), q.autoDelete(),
                        q.arguments(), admin.esProtegido(q.name())))
                .sorted(Comparator.comparing(ColaResponse::nombre))
                .toList();
    }

    @Override
    public ColaResponse obtenerCola(String nombre) {
        QueueInformation info = infoCola(nombre);
        return new ColaResponse(info.getName(), (long) info.getMessageCount(), info.getConsumerCount(),
                null, null, null, admin.esProtegido(nombre));
    }

    @Override
    public ColaResponse crearCola(CrearColaRequest r) {
        if (amqp(() -> amqpAdmin.getQueueInfo(r.nombre())) != null) {
            throw new ConflictoException("La cola '" + r.nombre() + "' ya existe");
        }
        if (r.tieneDeadLetter()) {
            exigirExchange(r.deadLetterExchange());
        }

        QueueBuilder builder = r.esDurable() ? QueueBuilder.durable(r.nombre()) : QueueBuilder.nonDurable(r.nombre());
        if (r.esAutoDelete()) {
            builder.autoDelete();
        }
        if (r.ttlMs() != null) {
            builder.ttl(r.ttlMs());
        }
        if (r.maxMensajes() != null) {
            builder.maxLength(r.maxMensajes());
        }
        if (r.tieneDeadLetter()) {
            builder.deadLetterExchange(r.deadLetterExchange());
            if (r.deadLetterRoutingKey() != null && !r.deadLetterRoutingKey().isBlank()) {
                builder.deadLetterRoutingKey(r.deadLetterRoutingKey());
            }
        }

        declarar(() -> amqpAdmin.declareQueue(builder.build()), "la cola '" + r.nombre() + "'");
        log.info("[ADMIN] Cola creada: {} (durable={}, ttlMs={}, dlx={})",
                r.nombre(), r.esDurable(), r.ttlMs(), r.deadLetterExchange());
        return obtenerCola(r.nombre());
    }

    @Override
    public void eliminarCola(String nombre, boolean soloSiVacia, boolean soloSiSinConsumidores) {
        exigirNoProtegido(nombre, "la cola");
        QueueInformation info = infoCola(nombre);
        if (soloSiVacia && info.getMessageCount() > 0) {
            throw new ConflictoException("La cola '" + nombre + "' tiene " + info.getMessageCount()
                    + " mensajes; no se elimina porque se pidió soloSiVacia=true");
        }
        if (soloSiSinConsumidores && info.getConsumerCount() > 0) {
            throw new ConflictoException("La cola '" + nombre + "' tiene " + info.getConsumerCount()
                    + " consumidores conectados; no se elimina porque se pidió soloSiSinConsumidores=true");
        }
        amqp(() -> amqpAdmin.deleteQueue(nombre));
        log.info("[ADMIN] Cola eliminada: {} (tenía {} mensajes)", nombre, info.getMessageCount());
    }

    @Override
    public PurgaResponse purgarCola(String nombre) {
        infoCola(nombre);
        int eliminados = amqp(() -> amqpAdmin.purgeQueue(nombre));
        log.info("[ADMIN] Cola purgada: {} ({} mensajes eliminados)", nombre, eliminados);
        return new PurgaResponse(nombre, eliminados);
    }

    // ================================================================== Exchanges

    @Override
    public List<ExchangeResponse> listarExchanges() {
        return management.listarExchanges().stream()
                .filter(e -> !e.esDelSistema())
                .map(this::aRespuesta)
                .sorted(Comparator.comparing(ExchangeResponse::nombre))
                .toList();
    }

    @Override
    public ExchangeResponse crearExchange(CrearExchangeRequest r) {
        if (management.buscarExchange(r.nombre()).isPresent()) {
            throw new ConflictoException("El exchange '" + r.nombre() + "' ya existe");
        }
        Exchange exchange = switch (r.tipo()) {
            case DIRECT -> new DirectExchange(r.nombre(), r.esDurable(), r.esAutoDelete());
            case TOPIC -> new TopicExchange(r.nombre(), r.esDurable(), r.esAutoDelete());
            case FANOUT -> new FanoutExchange(r.nombre(), r.esDurable(), r.esAutoDelete());
            case HEADERS -> new HeadersExchange(r.nombre(), r.esDurable(), r.esAutoDelete());
        };
        declarar(() -> amqpAdmin.declareExchange(exchange), "el exchange '" + r.nombre() + "'");
        log.info("[ADMIN] Exchange creado: {} ({})", r.nombre(), r.tipo().valor());
        return new ExchangeResponse(r.nombre(), r.tipo().valor(), r.esDurable(), r.esAutoDelete(),
                admin.esProtegido(r.nombre()));
    }

    @Override
    public void eliminarExchange(String nombre) {
        exigirNoProtegido(nombre, "el exchange");
        exigirExchange(nombre);
        amqp(() -> amqpAdmin.deleteExchange(nombre));
        log.info("[ADMIN] Exchange eliminado: {}", nombre);
    }

    // ================================================================== Bindings

    @Override
    public List<BindingResponse> listarBindings() {
        return management.listarBindings().stream()
                .filter(ManagementBinding::esDeColaExplicito)
                .map(b -> new BindingResponse(b.source(), b.destination(), b.routingKey()))
                .sorted(Comparator.comparing(BindingResponse::exchange).thenComparing(BindingResponse::cola))
                .toList();
    }

    @Override
    public BindingResponse crearBinding(BindingRequest r) {
        ManagementExchange exchange = exigirExchange(r.exchange());
        infoCola(r.cola());
        if (r.usaComodines() && !TipoExchange.TOPIC.valor().equals(exchange.type())) {
            throw new SolicitudInvalidaException("Los comodines '*' y '#' solo funcionan en exchanges topic; '"
                    + r.exchange() + "' es de tipo " + exchange.type());
        }
        if (existeBinding(r)) {
            throw new ConflictoException("El binding " + r.exchange() + " -> " + r.cola()
                    + " con routing key '" + r.routingKey() + "' ya existe");
        }
        declarar(() -> amqpAdmin.declareBinding(aBinding(r)), "el binding");
        log.info("[ADMIN] Binding creado: {} -[{}]-> {}", r.exchange(), r.routingKey(), r.cola());
        return new BindingResponse(r.exchange(), r.cola(), r.routingKey());
    }

    @Override
    public void eliminarBinding(BindingRequest r) {
        if (admin.esProtegido(r.exchange()) && admin.esProtegido(r.cola())) {
            throw new ConflictoException("El binding " + r.exchange() + " -> " + r.cola()
                    + " es parte de la topología de GestorCitas y no se puede eliminar");
        }
        if (!existeBinding(r)) {
            throw new RecursoNoEncontradoException("No existe el binding " + r.exchange() + " -> " + r.cola()
                    + " con routing key '" + r.routingKey() + "'");
        }
        amqp(() -> amqpAdmin.removeBinding(aBinding(r)));
        log.info("[ADMIN] Binding eliminado: {} -[{}]-> {}", r.exchange(), r.routingKey(), r.cola());
    }

    // ================================================================== Mensajes de prueba

    @Override
    public PublicacionResponse publicar(PublicarMensajeRequest r) {
        exigirExchange(r.exchange());
        boolean enrutable = management.listarBindings().stream()
                .anyMatch(b -> r.exchange().equals(b.source()) && coincide(b.routingKey(), r.routingKey()));

        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setMessageId(UUID.randomUUID().toString());
        Message mensaje = new Message(serializar(r), props);

        amqp(() -> rabbitTemplate.send(r.exchange(), r.routingKey(), mensaje));
        log.info("[ADMIN] Mensaje de prueba publicado en {} con routingKey={} id={} enrutable={}",
                r.exchange(), r.routingKey(), props.getMessageId(), enrutable);
        return new PublicacionResponse(props.getMessageId(), r.exchange(), r.routingKey(), enrutable);
    }

    // ================================================================== Apoyo

    private QueueInformation infoCola(String nombre) {
        QueueInformation info = amqp(() -> amqpAdmin.getQueueInfo(nombre));
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return info;
    }

    private ManagementExchange exigirExchange(String nombre) {
        return management.buscarExchange(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe"));
    }

    private void exigirNoProtegido(String nombre, String recurso) {
        if (admin.esProtegido(nombre)) {
            throw new ConflictoException("No se puede eliminar " + recurso + " '" + nombre
                    + "': es parte de la topología de GestorCitas (app.admin.recursos-protegidos)");
        }
    }

    private boolean existeBinding(BindingRequest r) {
        return management.listarBindings().stream().anyMatch(b ->
                r.exchange().equals(b.source()) && r.cola().equals(b.destination())
                        && "queue".equals(b.destinationType()) && r.routingKey().equals(b.routingKey()));
    }

    private static Binding aBinding(BindingRequest r) {
        return new Binding(r.cola(), Binding.DestinationType.QUEUE, r.exchange(), r.routingKey(), null);
    }

    /** Coincidencia de routing key con un patrón de binding (soporta los comodines de topic). */
    static boolean coincide(String patron, String routingKey) {
        if (patron == null) {
            return false;
        }
        String regex = patron
                .replace(".", "\\.")
                .replace("*", "[^.]+")
                .replace("#", ".*");
        return routingKey.matches("^" + regex + "$");
    }

    private byte[] serializar(PublicarMensajeRequest r) {
        try {
            return objectMapper.writeValueAsBytes(r.payload());
        } catch (JsonProcessingException ex) {
            throw new SolicitudInvalidaException("El payload no es un JSON válido: " + ex.getOriginalMessage());
        }
    }

    private ExchangeResponse aRespuesta(ManagementExchange e) {
        return new ExchangeResponse(e.name(), e.type(), e.durable(), e.autoDelete(), admin.esProtegido(e.name()));
    }

    /** Ejecuta una declaración AMQP traduciendo los rechazos del broker a 409. */
    private void declarar(Runnable declaracion, String recurso) {
        try {
            declaracion.run();
        } catch (AmqpConnectException ex) {
            throw new ServicioNoDisponibleException("RabbitMQ no está disponible", ex);
        } catch (AmqpException ex) {
            throw new ConflictoException("RabbitMQ rechazó crear " + recurso + ": " + causaRaiz(ex));
        }
    }

    private static <T> T amqp(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (AmqpConnectException ex) {
            throw new ServicioNoDisponibleException("RabbitMQ no está disponible", ex);
        }
    }

    private static void amqp(Runnable operacion) {
        amqp(() -> {
            operacion.run();
            return null;
        });
    }

    private static String causaRaiz(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}

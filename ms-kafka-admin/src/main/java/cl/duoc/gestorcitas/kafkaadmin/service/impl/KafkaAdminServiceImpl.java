package cl.duoc.gestorcitas.kafkaadmin.service.impl;

import cl.duoc.gestorcitas.kafkaadmin.config.KafkaAdminProperties;
import cl.duoc.gestorcitas.kafkaadmin.dto.*;
import cl.duoc.gestorcitas.kafkaadmin.exception.ConflictoException;
import cl.duoc.gestorcitas.kafkaadmin.exception.RecursoNoEncontradoException;
import cl.duoc.gestorcitas.kafkaadmin.exception.ServicioNoDisponibleException;
import cl.duoc.gestorcitas.kafkaadmin.exception.SolicitudInvalidaException;
import cl.duoc.gestorcitas.kafkaadmin.service.KafkaAdminService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownMemberIdException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Implementación con {@link AdminClient}. Cada operación espera la respuesta del clúster como máximo
 * {@code app.admin.timeout-segundos} y traduce los errores de Kafka a respuestas HTTP:
 * 404 (no existe), 409 (ya existe / protegido / grupo activo), 400 (rechazado por Kafka), 503 (clúster no responde).
 */
@Service
@SuppressWarnings("deprecation") // ConsumerGroupState / state(): API vigente en kafka-clients 3.x
public class KafkaAdminServiceImpl implements KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminServiceImpl.class);
    private static final String CLEANUP_POLICY = "cleanup.policy";

    private final AdminClient admin;
    private final KafkaAdminProperties props;

    public KafkaAdminServiceImpl(AdminClient admin, KafkaAdminProperties props) {
        this.admin = admin;
        this.props = props;
    }

    // ------------------------------------------------------------------ Clúster

    @Override
    public ClusterResponse cluster() {
        DescribeClusterResult r = admin.describeCluster();
        String id = esperar(r.clusterId(), "El clúster");
        Node controlador = esperar(r.controller(), "El clúster");
        List<BrokerResponse> brokers = esperar(r.nodes(), "El clúster").stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(KafkaAdminServiceImpl::broker)
                .toList();
        return new ClusterResponse(id, controlador == null || controlador.isEmpty() ? null : broker(controlador),
                brokers.size(), brokers);
    }

    // ------------------------------------------------------------------ Tópicos

    @Override
    public List<TopicoResumenResponse> listarTopicos() {
        Set<String> nombres = esperar(admin.listTopics().names(), "La lista de tópicos");
        if (nombres.isEmpty()) {
            return List.of();
        }
        return esperar(admin.describeTopics(nombres).allTopicNames(), "Los tópicos").values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(t -> new TopicoResumenResponse(t.name(), t.partitions().size(), factorReplicacion(t),
                        props.esProtegido(t.name())))
                .toList();
    }

    @Override
    public TopicoDetalleResponse obtenerTopico(String nombre) {
        TopicDescription descripcion = describir(nombre);
        List<TopicPartition> particiones = descripcion.partitions().stream()
                .map(p -> new TopicPartition(nombre, p.partition()))
                .toList();
        Map<TopicPartition, Long> iniciales = offsets(particiones, OffsetSpec.earliest());
        Map<TopicPartition, Long> finales = offsets(particiones, OffsetSpec.latest());

        List<ParticionResponse> detalle = descripcion.partitions().stream()
                .sorted(Comparator.comparingInt(TopicPartitionInfo::partition))
                .map(p -> {
                    TopicPartition tp = new TopicPartition(nombre, p.partition());
                    return new ParticionResponse(p.partition(),
                            p.leader() == null || p.leader().isEmpty() ? null : p.leader().id(),
                            ids(p.replicas()), ids(p.isr()),
                            iniciales.getOrDefault(tp, 0L), finales.getOrDefault(tp, 0L));
                })
                .toList();
        long registros = detalle.stream().mapToLong(p -> p.offsetFinal() - p.offsetInicial()).sum();

        return new TopicoDetalleResponse(nombre, props.esProtegido(nombre), factorReplicacion(descripcion),
                detalle, configuracionPropia(nombre), registros);
    }

    @Override
    public TopicoDetalleResponse crearTopico(CrearTopicoRequest request) {
        validarConfiguracion(request.configuracion(), false);
        NewTopic nuevo = new NewTopic(request.nombre(), request.particiones(), request.factorReplicacion().shortValue());
        if (request.configuracion() != null && !request.configuracion().isEmpty()) {
            nuevo.configs(request.configuracion());
        }
        esperar(admin.createTopics(List.of(nuevo)).all(), "El tópico " + request.nombre());
        log.info("[KafkaAdmin] Tópico creado nombre={} particiones={} replicacion={} config={}",
                request.nombre(), request.particiones(), request.factorReplicacion(), request.configuracion());
        return obtenerTopico(request.nombre());
    }

    @Override
    public TopicoDetalleResponse actualizarConfiguracion(String nombre, Map<String, String> configuracion) {
        validarConfiguracion(configuracion, true);
        if (props.esProtegido(nombre) && configuracion.containsKey(CLEANUP_POLICY)) {
            throw new ConflictoException("El tópico " + nombre + " está protegido: no se puede cambiar su "
                    + CLEANUP_POLICY + " (GestorCitas depende de su retención/compactación)");
        }
        describir(nombre); // 404 si no existe
        List<AlterConfigOp> operaciones = configuracion.entrySet().stream()
                .map(e -> e.getValue().isBlank()
                        ? new AlterConfigOp(new ConfigEntry(e.getKey(), null), AlterConfigOp.OpType.DELETE)
                        : new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        esperar(admin.incrementalAlterConfigs(Map.of(recurso, operaciones)).all(), "El tópico " + nombre);
        log.info("[KafkaAdmin] Configuración actualizada topico={} cambios={}", nombre, configuracion);
        return obtenerTopico(nombre);
    }

    @Override
    public TopicoDetalleResponse aumentarParticiones(String nombre, int total) {
        if (props.esProtegido(nombre)) {
            throw new ConflictoException("El tópico " + nombre + " está protegido: aumentar sus particiones cambiaría "
                    + "la partición asignada a cada clave (se pierde el orden por cita/usuario) y la paridad con su .DLT");
        }
        int actuales = describir(nombre).partitions().size();
        if (total <= actuales) {
            throw new SolicitudInvalidaException("El tópico " + nombre + " ya tiene " + actuales
                    + " particiones: Kafka solo permite aumentarlas (indica un total mayor a " + actuales + ")");
        }
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all(), "El tópico " + nombre);
        log.info("[KafkaAdmin] Particiones aumentadas topico={} de {} a {}", nombre, actuales, total);
        return obtenerTopico(nombre);
    }

    @Override
    public void eliminarTopico(String nombre) {
        if (props.esProtegido(nombre)) {
            throw new ConflictoException("El tópico " + nombre + " está protegido: GestorCitas depende de él");
        }
        esperar(admin.deleteTopics(List.of(nombre)).all(), "El tópico " + nombre);
        log.info("[KafkaAdmin] Tópico eliminado nombre={}", nombre);
    }

    // ------------------------------------------------------------------ Grupos de consumidores

    @Override
    public List<GrupoResumenResponse> listarGrupos() {
        return esperar(admin.listConsumerGroups().all(), "La lista de grupos").stream()
                .map(g -> new GrupoResumenResponse(g.groupId(), g.state().map(Enum::name).orElse("UNKNOWN")))
                .sorted(Comparator.comparing(GrupoResumenResponse::grupo))
                .toList();
    }

    @Override
    public GrupoDetalleResponse obtenerGrupo(String grupo) {
        ConsumerGroupDescription d = describirGrupo(grupo);
        if (d.state() == ConsumerGroupState.DEAD) {
            throw new RecursoNoEncontradoException("El grupo de consumidores " + grupo + " no existe");
        }
        List<MiembroResponse> miembros = d.members().stream()
                .map(m -> new MiembroResponse(m.consumerId(), m.clientId(), m.host(),
                        m.assignment().topicPartitions().stream()
                                .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                                .map(TopicPartition::toString)
                                .toList()))
                .sorted(Comparator.comparing(MiembroResponse::clientId))
                .toList();
        List<LagResponse> lag = lag(grupo);
        return new GrupoDetalleResponse(grupo, d.state().name(),
                d.coordinator() == null ? null : d.coordinator().id(),
                miembros, lag, lag.stream().mapToLong(LagResponse::lag).sum());
    }

    @Override
    public ReinicioOffsetsResponse reiniciarOffsets(String grupo, ReiniciarOffsetsRequest request) {
        ConsumerGroupDescription d = describirGrupo(grupo);
        if (!d.members().isEmpty()) {
            throw new ConflictoException("El grupo " + grupo + " tiene " + d.members().size()
                    + " consumidor(es) activo(s): detén sus instancias antes de reiniciar los offsets");
        }
        List<TopicPartition> particiones = describir(request.topico()).partitions().stream()
                .map(p -> new TopicPartition(request.topico(), p.partition()))
                .toList();

        Map<TopicPartition, Long> destino = switch (request.estrategia()) {
            case EARLIEST -> offsets(particiones, OffsetSpec.earliest());
            case LATEST -> offsets(particiones, OffsetSpec.latest());
            case OFFSET -> {
                Map<TopicPartition, Long> iniciales = offsets(particiones, OffsetSpec.earliest());
                Map<TopicPartition, Long> finales = offsets(particiones, OffsetSpec.latest());
                Map<TopicPartition, Long> ajustados = new HashMap<>();
                particiones.forEach(tp -> ajustados.put(tp,
                        Math.max(iniciales.get(tp), Math.min(request.offset(), finales.get(tp)))));
                yield ajustados;
            }
        };

        Map<TopicPartition, OffsetAndMetadata> nuevos = destino.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new OffsetAndMetadata(e.getValue())));
        esperar(admin.alterConsumerGroupOffsets(grupo, nuevos).all(), "El grupo " + grupo);
        log.info("[KafkaAdmin] Offsets reiniciados grupo={} topico={} estrategia={} offsets={}",
                grupo, request.topico(), request.estrategia(), destino);

        Map<Integer, Long> porParticion = new TreeMap<>();
        destino.forEach((tp, offset) -> porParticion.put(tp.partition(), offset));
        return new ReinicioOffsetsResponse(grupo, request.topico(), request.estrategia(), porParticion);
    }

    @Override
    public void eliminarGrupo(String grupo) {
        esperar(admin.deleteConsumerGroups(List.of(grupo)).all(), "El grupo de consumidores " + grupo);
        log.info("[KafkaAdmin] Grupo de consumidores eliminado grupo={}", grupo);
    }

    // ------------------------------------------------------------------ Apoyo

    private TopicDescription describir(String nombre) {
        return esperar(admin.describeTopics(List.of(nombre)).allTopicNames(), "El tópico " + nombre).get(nombre);
    }

    private ConsumerGroupDescription describirGrupo(String grupo) {
        return esperar(admin.describeConsumerGroups(List.of(grupo)).all(), "El grupo de consumidores " + grupo).get(grupo);
    }

    private List<LagResponse> lag(String grupo) {
        Map<TopicPartition, OffsetAndMetadata> confirmados = esperar(
                admin.listConsumerGroupOffsets(grupo).partitionsToOffsetAndMetadata(), "El grupo " + grupo);
        List<TopicPartition> conOffset = confirmados.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(Map.Entry::getKey)
                .toList();
        Map<TopicPartition, Long> finales = offsets(conOffset, OffsetSpec.latest());
        return conOffset.stream()
                .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                .map(tp -> {
                    long confirmado = confirmados.get(tp).offset();
                    long fin = finales.getOrDefault(tp, confirmado);
                    return new LagResponse(tp.topic(), tp.partition(), confirmado, fin, Math.max(0, fin - confirmado));
                })
                .toList();
    }

    private Map<TopicPartition, Long> offsets(Collection<TopicPartition> particiones, OffsetSpec spec) {
        if (particiones.isEmpty()) {
            return Map.of();
        }
        Map<TopicPartition, OffsetSpec> consulta = new HashMap<>();
        particiones.forEach(tp -> consulta.put(tp, spec));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> r =
                esperar(admin.listOffsets(consulta).all(), "Los offsets");
        Map<TopicPartition, Long> salida = new HashMap<>();
        r.forEach((tp, info) -> salida.put(tp, info.offset()));
        return salida;
    }

    /** Configuraciones definidas específicamente para el tópico (no las heredadas del broker). */
    private Map<String, String> configuracionPropia(String nombre) {
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all(), "El tópico " + nombre).get(recurso);
        Map<String, String> propias = new TreeMap<>();
        config.entries().stream()
                .filter(e -> e.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
                .forEach(e -> propias.put(e.name(), e.value()));
        return propias;
    }

    private void validarConfiguracion(Map<String, String> configuracion, boolean permiteVacios) {
        if (configuracion == null || configuracion.isEmpty()) {
            return;
        }
        List<String> noPermitidas = configuracion.keySet().stream()
                .filter(k -> !props.configuracionesPermitidas().contains(k))
                .sorted()
                .toList();
        if (!noPermitidas.isEmpty()) {
            throw new SolicitudInvalidaException("Configuraciones no permitidas: " + noPermitidas
                    + ". Permitidas: " + props.configuracionesPermitidas().stream().sorted().toList());
        }
        List<String> sinValor = configuracion.entrySet().stream()
                .filter(e -> e.getValue() == null || (!permiteVacios && e.getValue().isBlank()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!sinValor.isEmpty()) {
            throw new SolicitudInvalidaException("Configuraciones sin valor: " + sinValor);
        }
    }

    private static int factorReplicacion(TopicDescription t) {
        return t.partitions().isEmpty() ? 0 : t.partitions().get(0).replicas().size();
    }

    private static List<Integer> ids(List<Node> nodos) {
        return nodos.stream().filter(Objects::nonNull).map(Node::id).sorted().toList();
    }

    private static BrokerResponse broker(Node n) {
        return new BrokerResponse(n.id(), n.host(), n.port(), n.hasRack() ? n.rack() : null);
    }

    /** Espera el resultado del clúster y traduce los errores de Kafka a excepciones HTTP. */
    private <T> T esperar(KafkaFuture<T> futuro, String recurso) {
        try {
            return futuro.get(props.timeoutSegundos(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException("Operación interrumpida", e);
        } catch (TimeoutException e) {
            throw new ServicioNoDisponibleException("El clúster Kafka no respondió en " + props.timeoutSegundos() + " s", e);
        } catch (ExecutionException e) {
            throw traducir(e.getCause() == null ? e : e.getCause(), recurso);
        }
    }

    static RuntimeException traducir(Throwable causa, String recurso) {
        if (causa instanceof UnknownTopicOrPartitionException || causa instanceof GroupIdNotFoundException) {
            return new RecursoNoEncontradoException(recurso + " no existe");
        }
        if (causa instanceof TopicExistsException) {
            return new ConflictoException(recurso + " ya existe");
        }
        if (causa instanceof GroupNotEmptyException || causa instanceof UnknownMemberIdException) {
            return new ConflictoException(recurso + " tiene consumidores activos: detén sus instancias antes");
        }
        if (causa instanceof InvalidPartitionsException || causa instanceof InvalidReplicationFactorException
                || causa instanceof InvalidConfigurationException || causa instanceof InvalidRequestException
                || causa instanceof PolicyViolationException || causa instanceof InvalidTopicException) {
            return new SolicitudInvalidaException(causa.getMessage(), causa);
        }
        return new ServicioNoDisponibleException("Kafka no pudo completar la operación: " + causa.getMessage(), causa);
    }
}

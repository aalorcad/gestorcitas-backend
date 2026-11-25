package cl.duoc.gestorcitas.bff.service.impl;

import cl.duoc.gestorcitas.bff.client.KafkaAdminClient;
import cl.duoc.gestorcitas.bff.service.KafkaAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public class KafkaAdminServiceImpl implements KafkaAdminService {

    private static final String CLUSTER = "/cluster";
    private static final String TOPICOS = "/topics";
    private static final String TOPICO = "/topics/{nombre}";
    private static final String CONFIG_TOPICO = "/topics/{nombre}/config";
    private static final String PARTICIONES_TOPICO = "/topics/{nombre}/partitions";
    private static final String GRUPOS = "/consumer-groups";
    private static final String GRUPO = "/consumer-groups/{grupo}";
    private static final String REINICIO_GRUPO = "/consumer-groups/{grupo}/reset-offsets";

    private final KafkaAdminClient client;

    public KafkaAdminServiceImpl(KafkaAdminClient client) {
        this.client = client;
    }

    @Override
    public JsonNode cluster() {
        return client.get(CLUSTER);
    }

    @Override
    public JsonNode listarTopicos() {
        return client.get(TOPICOS);
    }

    @Override
    public JsonNode obtenerTopico(String nombre) {
        return client.get(TOPICO, nombre);
    }

    @Override
    public JsonNode crearTopico(JsonNode request) {
        return client.post(TOPICOS, request);
    }

    @Override
    public JsonNode actualizarConfiguracion(String nombre, JsonNode request) {
        return client.patch(CONFIG_TOPICO, request, nombre);
    }

    @Override
    public JsonNode aumentarParticiones(String nombre, JsonNode request) {
        return client.post(PARTICIONES_TOPICO, request, nombre);
    }

    @Override
    public void eliminarTopico(String nombre) {
        client.delete(TOPICO, nombre);
    }

    @Override
    public JsonNode listarGrupos() {
        return client.get(GRUPOS);
    }

    @Override
    public JsonNode obtenerGrupo(String grupo) {
        return client.get(GRUPO, grupo);
    }

    @Override
    public JsonNode reiniciarOffsets(String grupo, JsonNode request) {
        return client.post(REINICIO_GRUPO, request, grupo);
    }

    @Override
    public void eliminarGrupo(String grupo) {
        client.delete(GRUPO, grupo);
    }
}

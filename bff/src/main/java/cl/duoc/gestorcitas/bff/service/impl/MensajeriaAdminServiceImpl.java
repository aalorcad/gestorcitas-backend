package cl.duoc.gestorcitas.bff.service.impl;

import cl.duoc.gestorcitas.bff.client.MensajeriaAdminClient;
import cl.duoc.gestorcitas.bff.service.MensajeriaAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MensajeriaAdminServiceImpl implements MensajeriaAdminService {

    private static final String COLAS = "/queues";
    private static final String COLA = "/queues/{nombre}";
    private static final String MENSAJES_COLA = "/queues/{nombre}/messages";
    private static final String EXCHANGES = "/exchanges";
    private static final String EXCHANGE = "/exchanges/{nombre}";
    private static final String BINDINGS = "/bindings";
    private static final String MENSAJES = "/messages";

    private final MensajeriaAdminClient client;

    public MensajeriaAdminServiceImpl(MensajeriaAdminClient client) {
        this.client = client;
    }

    @Override
    public JsonNode listarColas() {
        return client.get(COLAS);
    }

    @Override
    public JsonNode obtenerCola(String nombre) {
        return client.get(COLA, nombre);
    }

    @Override
    public JsonNode crearCola(JsonNode request) {
        return client.post(COLAS, request);
    }

    @Override
    public void eliminarCola(String nombre, boolean soloSiVacia, boolean soloSiSinConsumidores) {
        client.delete(COLA, Map.of(
                "soloSiVacia", String.valueOf(soloSiVacia),
                "soloSiSinConsumidores", String.valueOf(soloSiSinConsumidores)), nombre);
    }

    @Override
    public JsonNode purgarCola(String nombre) {
        return client.deleteConRespuesta(MENSAJES_COLA, nombre);
    }

    @Override
    public JsonNode listarExchanges() {
        return client.get(EXCHANGES);
    }

    @Override
    public JsonNode crearExchange(JsonNode request) {
        return client.post(EXCHANGES, request);
    }

    @Override
    public void eliminarExchange(String nombre) {
        client.delete(EXCHANGE, Map.of(), nombre);
    }

    @Override
    public JsonNode listarBindings() {
        return client.get(BINDINGS);
    }

    @Override
    public JsonNode crearBinding(JsonNode request) {
        return client.post(BINDINGS, request);
    }

    @Override
    public void eliminarBinding(String exchange, String cola, String routingKey) {
        // Las variables se envían como valores de plantilla para que '#' y '*' se codifiquen correctamente
        Map<String, String> query = new LinkedHashMap<>();
        query.put("exchange", "{exchange}");
        query.put("cola", "{cola}");
        query.put("routingKey", "{routingKey}");
        client.delete(BINDINGS, query, exchange, cola, routingKey == null ? "" : routingKey);
    }

    @Override
    public JsonNode publicarMensaje(JsonNode request) {
        return client.post(MENSAJES, request);
    }
}

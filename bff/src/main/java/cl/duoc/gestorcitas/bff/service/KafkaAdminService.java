package cl.duoc.gestorcitas.bff.service;

import com.fasterxml.jackson.databind.JsonNode;

/** Administración de Kafka para el rol Admin (delegada en ms-kafka-admin). */
public interface KafkaAdminService {

    JsonNode cluster();

    JsonNode listarTopicos();

    JsonNode obtenerTopico(String nombre);

    JsonNode crearTopico(JsonNode request);

    JsonNode actualizarConfiguracion(String nombre, JsonNode request);

    JsonNode aumentarParticiones(String nombre, JsonNode request);

    void eliminarTopico(String nombre);

    JsonNode listarGrupos();

    JsonNode obtenerGrupo(String grupo);

    JsonNode reiniciarOffsets(String grupo, JsonNode request);

    void eliminarGrupo(String grupo);
}

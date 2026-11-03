package cl.duoc.gestorcitas.bff.service;

import com.fasterxml.jackson.databind.JsonNode;

/** Administración de RabbitMQ para el rol Admin (delegada en ms-rabbit-admin). */
public interface MensajeriaAdminService {

    JsonNode listarColas();

    JsonNode obtenerCola(String nombre);

    JsonNode crearCola(JsonNode request);

    void eliminarCola(String nombre, boolean soloSiVacia, boolean soloSiSinConsumidores);

    JsonNode purgarCola(String nombre);

    JsonNode listarExchanges();

    JsonNode crearExchange(JsonNode request);

    void eliminarExchange(String nombre);

    JsonNode listarBindings();

    JsonNode crearBinding(JsonNode request);

    void eliminarBinding(String exchange, String cola, String routingKey);

    JsonNode publicarMensaje(JsonNode request);
}

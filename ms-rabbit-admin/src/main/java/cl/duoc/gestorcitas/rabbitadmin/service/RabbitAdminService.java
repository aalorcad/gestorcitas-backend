package cl.duoc.gestorcitas.rabbitadmin.service;

import cl.duoc.gestorcitas.rabbitadmin.dto.*;

import java.util.List;

/**
 * Operaciones de administración de RabbitMQ.
 * Los controladores solo llaman a estos métodos de alto nivel y no conocen la librería de RabbitMQ.
 */
public interface RabbitAdminService {

    // ------------------------------------------------------------------ Colas
    List<ColaResponse> listarColas();

    ColaResponse obtenerCola(String nombre);

    ColaResponse crearCola(CrearColaRequest request);

    void eliminarCola(String nombre, boolean soloSiVacia, boolean soloSiSinConsumidores);

    PurgaResponse purgarCola(String nombre);

    // ------------------------------------------------------------------ Exchanges
    List<ExchangeResponse> listarExchanges();

    ExchangeResponse crearExchange(CrearExchangeRequest request);

    void eliminarExchange(String nombre);

    // ------------------------------------------------------------------ Bindings
    List<BindingResponse> listarBindings();

    BindingResponse crearBinding(BindingRequest request);

    void eliminarBinding(BindingRequest request);

    // ------------------------------------------------------------------ Mensajes de prueba
    PublicacionResponse publicar(PublicarMensajeRequest request);
}

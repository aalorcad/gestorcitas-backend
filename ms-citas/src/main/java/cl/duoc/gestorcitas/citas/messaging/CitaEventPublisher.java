package cl.duoc.gestorcitas.citas.messaging;

import cl.duoc.gestorcitas.citas.entity.Cita;

/**
 * Puerto de salida que usa la capa de negocio para avisar lo que ocurrió con una cita.
 * <p>
 * El servicio de citas solo conoce esta interfaz: no sabe de exchanges, routing keys ni de RabbitMQ.
 * La implementación ({@code RabbitCitaEventPublisher}) publica DESPUÉS del commit de la transacción,
 * de modo que un fallo del broker nunca revierte ni bloquea una reserva.
 */
public interface CitaEventPublisher {

    /** Publica el cambio de estado de la cita (exchange topic, se usa para notificaciones). */
    void publicarCambioDeEstado(Cita cita, TipoEventoCita tipo);

    /** Solicita la generación del comprobante de la cita (exchange direct). */
    void solicitarComprobante(Cita cita);
}

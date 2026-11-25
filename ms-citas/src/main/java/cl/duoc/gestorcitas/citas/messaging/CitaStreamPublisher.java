package cl.duoc.gestorcitas.citas.messaging;

import cl.duoc.gestorcitas.citas.entity.Cita;

/**
 * Puerto de salida hacia el streaming de eventos (Apache Kafka, EP5).
 * <p>
 * A diferencia de {@link CitaEventPublisher} (RabbitMQ: tareas puntuales como correos y comprobantes),
 * este flujo registra TODOS los cambios de estado de las citas en un log durable y re-leíble, particionado
 * por {@code citaId}, que consumen varios grupos de forma independiente (analítica e historial).
 * La implementación publica DESPUÉS del commit y nunca hace fallar la operación de negocio.
 */
public interface CitaStreamPublisher {

    /** Publica el cambio de estado de la cita en el tópico de eventos de citas. */
    void publicar(Cita cita, TipoEventoCita tipo);
}

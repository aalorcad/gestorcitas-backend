package cl.duoc.gestorcitas.notificaciones.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración centralizada de la mensajería de ms-notificaciones ({@code app.messaging} en application.yml).
 * Ninguna clase escribe nombres de colas o exchanges a mano: todos salen de este record.
 *
 * @param deadLetterExchange   exchange direct que recibe los mensajes rechazados y los enruta a cada DLQ
 * @param reintentos           política de reintentos de errores recuperables
 * @param notificacionesCitas  flujo: eventos de citas -> notificación por correo al paciente
 * @param comprobantesCitas    flujo: solicitud de comprobante -> generación del ticket de la cita
 * @param auditoriaUsuarios    flujo: eventos de usuarios -> registro de auditoría
 */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
        String deadLetterExchange,
        Reintentos reintentos,
        Flujo notificacionesCitas,
        Flujo comprobantesCitas,
        Flujo auditoriaUsuarios
) {

    /**
     * @param maximo   número total de intentos (el primero + reintentos) antes de enviar a la DLQ
     * @param esperaMs tiempo que el mensaje espera en la cola de reintento antes de volver a procesarse
     * @param header   header donde se lleva el número de intento
     */
    public record Reintentos(int maximo, int esperaMs, String header) {
    }

    /**
     * Un caso de uso de mensajería completo.
     *
     * @param exchange      exchange de origen (lo publica otro microservicio)
     * @param routingKey    patrón/clave con la que la cola se enlaza al exchange
     * @param cola          cola principal que consume este servicio
     * @param colaReintento cola de espera (TTL) que devuelve el mensaje a la cola principal
     * @param dlq           dead letter queue del flujo
     */
    public record Flujo(String exchange, String routingKey, String cola, String colaReintento, String dlq) {
    }
}

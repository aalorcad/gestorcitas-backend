package cl.duoc.gestorcitas.citas.messaging.config;

import cl.duoc.gestorcitas.citas.messaging.TipoEventoCita;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges y routing keys de ms-citas, leídos desde {@code app.messaging} (application.yml).
 * Ninguna clase de negocio escribe estos nombres a mano: todas los obtienen de aquí.
 *
 * @param habilitado  false desactiva la publicación (tests o entornos sin broker)
 * @param exchanges   exchanges donde publica ms-citas
 * @param routingKeys routing keys de cada tipo de mensaje
 */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(boolean habilitado, Exchanges exchanges, RoutingKeys routingKeys) {

    /**
     * @param citasEventos      exchange TOPIC con los cambios de estado de las citas
     * @param citasComprobantes exchange DIRECT con las solicitudes de comprobante
     */
    public record Exchanges(String citasEventos, String citasComprobantes) {
    }

    public record RoutingKeys(String citaCreada,
                              String citaConfirmada,
                              String citaCancelada,
                              String citaAtendida,
                              String comprobanteGenerar) {

        /** Routing key que corresponde a cada tipo de evento de cita. */
        public String paraEvento(TipoEventoCita tipo) {
            return switch (tipo) {
                case CREADA -> citaCreada;
                case CONFIRMADA -> citaConfirmada;
                case CANCELADA -> citaCancelada;
                case ATENDIDA -> citaAtendida;
            };
        }
    }
}

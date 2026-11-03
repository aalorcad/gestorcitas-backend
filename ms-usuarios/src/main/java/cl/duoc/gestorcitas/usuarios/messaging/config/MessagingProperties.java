package cl.duoc.gestorcitas.usuarios.messaging.config;

import cl.duoc.gestorcitas.usuarios.messaging.TipoEventoUsuario;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchange y routing keys de ms-usuarios, leídos desde {@code app.messaging} (application.yml).
 *
 * @param habilitado  false desactiva la publicación (tests o entornos sin broker)
 * @param exchanges   exchanges donde publica ms-usuarios
 * @param routingKeys routing keys de cada tipo de evento
 */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(boolean habilitado, Exchanges exchanges, RoutingKeys routingKeys) {

    /** @param usuariosEventos exchange TOPIC con los eventos de usuarios */
    public record Exchanges(String usuariosEventos) {
    }

    public record RoutingKeys(String usuarioRegistrado, String usuarioActivado, String usuarioDesactivado) {

        public String paraEvento(TipoEventoUsuario tipo) {
            return switch (tipo) {
                case REGISTRADO -> usuarioRegistrado;
                case ACTIVADO -> usuarioActivado;
                case DESACTIVADO -> usuarioDesactivado;
            };
        }
    }
}

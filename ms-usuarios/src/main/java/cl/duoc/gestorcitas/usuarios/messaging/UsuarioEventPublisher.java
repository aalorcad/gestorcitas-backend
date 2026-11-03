package cl.duoc.gestorcitas.usuarios.messaging;

import cl.duoc.gestorcitas.usuarios.entity.Usuario;

/**
 * Puerto de salida de la capa de negocio de usuarios.
 * Los servicios solo conocen esta interfaz; la implementación RabbitMQ publica después del commit.
 */
public interface UsuarioEventPublisher {

    /**
     * Publica un evento del ciclo de vida del usuario (exchange topic de usuarios).
     *
     * @param usuario   usuario afectado
     * @param tipo      qué ocurrió
     * @param actorId   oid de quien ejecutó la acción (el mismo usuario o un administrador)
     */
    void publicar(Usuario usuario, TipoEventoUsuario tipo, String actorId);
}

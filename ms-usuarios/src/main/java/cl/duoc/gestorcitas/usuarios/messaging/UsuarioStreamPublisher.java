package cl.duoc.gestorcitas.usuarios.messaging;

import cl.duoc.gestorcitas.usuarios.entity.Usuario;

/**
 * Puerto de salida hacia el streaming de estado de usuarios (Apache Kafka, EP5).
 * <p>
 * Publica una "foto" del estado actual del usuario con clave {@code usuarioId} en un tópico compactado:
 * Kafka conserva siempre el último estado de cada usuario, de modo que un consumidor nuevo puede
 * reconstruir el estado completo leyendo el tópico desde el inicio.
 * La implementación publica DESPUÉS del commit y nunca hace fallar la operación de negocio.
 */
public interface UsuarioStreamPublisher {

    /**
     * @param usuario usuario con su estado ya guardado
     * @param tipo    cambio que originó la publicación
     */
    void publicar(Usuario usuario, TipoEventoUsuario tipo);
}

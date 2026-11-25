package cl.duoc.gestorcitas.usuarios.messaging.kafka;

import cl.duoc.gestorcitas.usuarios.entity.RolUsuario;
import cl.duoc.gestorcitas.usuarios.entity.Usuario;
import cl.duoc.gestorcitas.usuarios.messaging.TipoEventoUsuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mensaje JSON publicado en el tópico compactado {@code gestorcitas.usuarios.estado}.
 * Representa el estado COMPLETO del usuario después del cambio (no solo la diferencia), porque con
 * la compactación Kafka conserva únicamente el último registro de cada clave ({@code usuarioId}).
 * Contrato con el consumidor ms-analitica (grupo "analitica").
 *
 * @param eventoId identificador único; los consumidores lo usan para descartar duplicados
 */
public record UsuarioEstadoKafka(
        String eventoId,
        TipoEventoUsuario tipo,
        Long usuarioId,
        String email,
        String nombre,
        List<String> roles,
        boolean activo,
        LocalDateTime actualizadoEn
) {
    public static UsuarioEstadoKafka desde(Usuario u, TipoEventoUsuario tipo, LocalDateTime actualizadoEn) {
        List<String> roles = u.getRoles() == null ? List.of()
                : u.getRoles().stream().map(RolUsuario::getAppRole).sorted().toList();
        return new UsuarioEstadoKafka(UUID.randomUUID().toString(), tipo, u.getId(), u.getEmail(), u.getNombre(),
                roles, u.isActivo(), actualizadoEn);
    }
}

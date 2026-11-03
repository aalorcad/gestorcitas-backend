package cl.duoc.gestorcitas.usuarios.messaging.event;

import cl.duoc.gestorcitas.usuarios.entity.RolUsuario;
import cl.duoc.gestorcitas.usuarios.entity.Usuario;
import cl.duoc.gestorcitas.usuarios.messaging.TipoEventoUsuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mensaje JSON publicado en el exchange de eventos de usuarios.
 * Contrato con el consumidor de auditoría de ms-notificaciones.
 */
public record UsuarioEvento(
        String eventoId,
        TipoEventoUsuario tipo,
        Long usuarioId,
        String email,
        String nombre,
        List<String> roles,
        boolean activo,
        String actorId,
        LocalDateTime ocurridoEn
) {
    public static UsuarioEvento desde(Usuario u, TipoEventoUsuario tipo, String actorId, LocalDateTime ocurridoEn) {
        List<String> roles = u.getRoles() == null ? List.of()
                : u.getRoles().stream().map(RolUsuario::getAppRole).sorted().toList();
        return new UsuarioEvento(UUID.randomUUID().toString(), tipo, u.getId(), u.getEmail(), u.getNombre(),
                roles, u.isActivo(), actorId, ocurridoEn);
    }
}

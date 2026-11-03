package cl.duoc.gestorcitas.notificaciones.messaging.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/** Evento de usuario publicado por ms-usuarios (exchange topic {@code gestorcitas.usuarios.eventos}). */
public record UsuarioEvento(
        @NotBlank String eventoId,
        @NotBlank String tipo,
        @NotNull Long usuarioId,
        @NotBlank String email,
        String nombre,
        List<String> roles,
        boolean activo,
        String actorId,
        LocalDateTime ocurridoEn
) {
}

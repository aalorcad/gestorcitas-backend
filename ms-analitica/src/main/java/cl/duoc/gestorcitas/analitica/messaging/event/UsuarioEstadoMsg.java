package cl.duoc.gestorcitas.analitica.messaging.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado de un usuario leído del tópico COMPACTADO {@code gestorcitas.usuarios.estado}
 * (lo publica ms-usuarios, clave = usuarioId). Cada registro trae el estado completo del usuario.
 */
public record UsuarioEstadoMsg(
        @NotBlank String eventoId,
        @NotNull @Pattern(regexp = "REGISTRADO|ACTIVADO|DESACTIVADO") String tipo,
        @NotNull Long usuarioId,
        String email,
        String nombre,
        List<String> roles,
        boolean activo,
        @NotNull LocalDateTime actualizadoEn
) {
}

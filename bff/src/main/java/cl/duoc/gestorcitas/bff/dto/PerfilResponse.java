package cl.duoc.gestorcitas.bff.dto;

import java.time.Instant;
import java.util.List;

/** Respuesta de /api/me: demuestra que el BFF validó el token y leyó sus claims. */
public record PerfilResponse(
        String id,
        String nombre,
        String email,
        List<String> roles,
        List<String> scopes,
        String tenantId,
        String issuer,
        List<String> audience,
        Instant expiraEn
) {
}

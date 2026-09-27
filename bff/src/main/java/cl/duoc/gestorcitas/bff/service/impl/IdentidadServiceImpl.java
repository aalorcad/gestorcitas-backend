package cl.duoc.gestorcitas.bff.service.impl;

import cl.duoc.gestorcitas.bff.config.EntraIdProperties;
import cl.duoc.gestorcitas.bff.dto.PerfilResponse;
import cl.duoc.gestorcitas.bff.dto.UsuarioAutenticado;
import cl.duoc.gestorcitas.bff.security.EntraRoles;
import cl.duoc.gestorcitas.bff.service.IdentidadService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class IdentidadServiceImpl implements IdentidadService {

    private final EntraIdProperties entra;

    public IdentidadServiceImpl(EntraIdProperties entra) {
        this.entra = entra;
    }

    @Override
    public UsuarioAutenticado desdeToken(Jwt jwt) {
        // "oid" identifica al usuario de forma estable en el tenant; "sub" es por aplicación.
        String id = firstNonBlank(jwt.getClaimAsString("oid"), jwt.getSubject());
        String email = firstNonBlank(
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("upn"));
        String nombre = firstNonBlank(jwt.getClaimAsString("name"), email);
        List<String> roles = EntraRoles.de(jwt, entra.defaultRole());
        return new UsuarioAutenticado(
                id,
                nombre,
                email == null ? null : email.toLowerCase(Locale.ROOT),
                roles);
    }

    @Override
    public PerfilResponse perfilToken(Jwt jwt) {
        UsuarioAutenticado u = desdeToken(jwt);
        String scp = jwt.getClaimAsString("scp");
        return new PerfilResponse(
                u.id(),
                u.nombre(),
                u.email(),
                u.roles(),
                scp == null ? List.of() : Arrays.asList(scp.split(" ")),
                jwt.getClaimAsString("tid"),
                jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
                jwt.getAudience(),
                jwt.getExpiresAt());
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}

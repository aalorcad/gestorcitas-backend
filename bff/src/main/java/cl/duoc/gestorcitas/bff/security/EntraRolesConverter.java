package cl.duoc.gestorcitas.bff.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Convierte los claims de Entra ID en authorities de Spring:
 *  - "roles" (App Roles asignados al usuario)  -> ROLE_Paciente, ROLE_Medico, ROLE_Admin
 *  - "scp"   (scopes delegados)                -> SCOPE_access_as_user
 */
public class EntraRolesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        }
        String scp = jwt.getClaimAsString("scp");
        if (scp != null) {
            for (String s : scp.split(" ")) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
            }
        }
        String nombre = jwt.getClaimAsString("name");
        return new JwtAuthenticationToken(jwt, authorities, nombre != null ? nombre : jwt.getSubject());
    }
}

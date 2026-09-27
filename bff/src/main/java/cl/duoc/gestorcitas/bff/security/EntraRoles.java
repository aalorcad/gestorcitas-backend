package cl.duoc.gestorcitas.bff.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Roles efectivos del usuario a partir del claim "roles" (App Roles de Entra ID).
 * Si el token no trae roles y hay un rol por defecto configurado (autoregistro en
 * External ID), se usa ese rol. Así un paciente que crea su cuenta puede operar sin
 * que un administrador le asigne el rol a mano.
 */
public final class EntraRoles {

    private EntraRoles() {
    }

    public static List<String> de(Jwt jwt, String defaultRole) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            return roles;
        }
        return defaultRole == null || defaultRole.isBlank() ? List.of() : List.of(defaultRole);
    }
}

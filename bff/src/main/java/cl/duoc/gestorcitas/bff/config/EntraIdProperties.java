package cl.duoc.gestorcitas.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Parámetros de Microsoft Entra ID usados para validar el access token.
 *
 * Funciona con los dos tipos de tenant:
 *  - Workforce (empresa): issuer y JWKS se derivan del Tenant ID (login.microsoftonline.com).
 *  - External ID (clientes, con autoregistro): se definen issuerUri y jwkSetUri
 *    con los valores de https://&lt;subdominio&gt;.ciamlogin.com/&lt;tenant&gt;/v2.0/.well-known/openid-configuration
 *
 * @param tenantId       Directory (tenant) ID
 * @param audiences      valores aceptados en el claim "aud" (Client ID de la API y/o api://...)
 * @param requiredScope  scope delegado que debe venir en "scp" (ej. access_as_user)
 * @param issuerUri      issuer explícito (opcional)
 * @param jwkSetUri      URL de las llaves públicas (opcional)
 * @param defaultRole    rol que recibe un usuario autenticado sin App Roles asignados,
 *                       ej. "Paciente" para quien se registra solo (opcional)
 */
@ConfigurationProperties(prefix = "app.security.entra")
public record EntraIdProperties(String tenantId, List<String> audiences, String requiredScope,
                                String issuerUri, String jwkSetUri, String defaultRole) {

    public String issuer() {
        return hasText(issuerUri) ? issuerUri : "https://login.microsoftonline.com/" + tenantId + "/v2.0";
    }

    public String jwks() {
        return hasText(jwkSetUri) ? jwkSetUri : "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}

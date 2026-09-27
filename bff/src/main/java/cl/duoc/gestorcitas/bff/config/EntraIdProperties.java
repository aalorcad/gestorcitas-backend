package cl.duoc.gestorcitas.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Parámetros de Microsoft Entra ID usados para validar el access token.
 *
 * @param tenantId       Directory (tenant) ID
 * @param audiences      valores aceptados en el claim "aud" (Client ID de la API y/o api://...)
 * @param requiredScope  scope delegado que debe venir en "scp" (ej. access_as_user)
 */
@ConfigurationProperties(prefix = "app.security.entra")
public record EntraIdProperties(String tenantId, List<String> audiences, String requiredScope) {

    public String issuer() {
        return "https://login.microsoftonline.com/" + tenantId + "/v2.0";
    }

    public String jwkSetUri() {
        return "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
    }
}

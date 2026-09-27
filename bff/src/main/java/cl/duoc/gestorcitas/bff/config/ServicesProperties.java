package cl.duoc.gestorcitas.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** URLs internas (red privada) de los microservicios. */
@ConfigurationProperties(prefix = "app.services")
public record ServicesProperties(String catalogoUrl, String citasUrl, String usuariosUrl) {
}

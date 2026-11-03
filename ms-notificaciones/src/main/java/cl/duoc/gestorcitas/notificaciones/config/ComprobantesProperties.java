package cl.duoc.gestorcitas.notificaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param directorio carpeta donde se guardan los comprobantes generados */
@ConfigurationProperties(prefix = "app.comprobantes")
public record ComprobantesProperties(String directorio) {
}

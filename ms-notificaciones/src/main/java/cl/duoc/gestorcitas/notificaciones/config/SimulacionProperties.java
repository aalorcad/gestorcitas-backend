package cl.duoc.gestorcitas.notificaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Apoyo para la demostración de reintentos y DLQ.
 *
 * @param dominioCorreoCaido los correos de este dominio simulan un servidor de correo caído (vacío = desactivado)
 */
@ConfigurationProperties(prefix = "app.simulacion")
public record SimulacionProperties(String dominioCorreoCaido) {

    public boolean correoCaidoPara(String email) {
        return dominioCorreoCaido != null && !dominioCorreoCaido.isBlank()
                && email != null && email.toLowerCase().endsWith("@" + dominioCorreoCaido.toLowerCase());
    }
}

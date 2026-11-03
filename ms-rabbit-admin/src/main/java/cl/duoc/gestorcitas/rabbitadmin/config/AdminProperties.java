package cl.duoc.gestorcitas.rabbitadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

/**
 * Reglas de administración ({@code app.admin}).
 *
 * @param recursosProtegidos colas y exchanges de GestorCitas que la API no permite eliminar
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(List<String> recursosProtegidos) {

    public AdminProperties {
        recursosProtegidos = recursosProtegidos == null ? List.of() : List.copyOf(recursosProtegidos);
    }

    public boolean esProtegido(String nombre) {
        return Set.copyOf(recursosProtegidos).contains(nombre);
    }
}

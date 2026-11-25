package cl.duoc.gestorcitas.kafkaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reglas de administración, leídas desde {@code app.admin} (application.yml).
 *
 * @param timeoutSegundos           espera máxima de cada operación del AdminClient
 * @param configuracionesPermitidas claves de configuración de tópico que la API acepta al crear o modificar
 * @param topicosSistema            tópicos de los que depende GestorCitas, con su diseño (particiones, réplicas,
 *                                  retención). Se crean al arrancar si no existen y quedan PROTEGIDOS: no se pueden
 *                                  eliminar, ni aumentar sus particiones (rompería el orden por clave y la paridad
 *                                  con su .DLT), ni cambiar su cleanup.policy
 */
@ConfigurationProperties(prefix = "app.admin")
public record KafkaAdminProperties(int timeoutSegundos, Set<String> configuracionesPermitidas,
                                   List<TopicoSistema> topicosSistema) {

    /**
     * @param configuracion propiedades del tópico (cleanup.policy, retention.ms, min.insync.replicas, ...)
     */
    public record TopicoSistema(String nombre, int particiones, int replicas, Map<String, String> configuracion) {
    }

    public boolean esProtegido(String topico) {
        return topico.startsWith("__")
                || (topicosSistema != null && topicosSistema.stream().anyMatch(t -> t.nombre().equals(topico)));
    }
}

package cl.duoc.gestorcitas.kafkaadmin.dto;

import java.util.List;

/**
 * Estado del clúster.
 *
 * @param controlador broker que coordina el clúster (elige líderes de partición)
 */
public record ClusterResponse(String clusterId, BrokerResponse controlador, int totalBrokers, List<BrokerResponse> brokers) {
}

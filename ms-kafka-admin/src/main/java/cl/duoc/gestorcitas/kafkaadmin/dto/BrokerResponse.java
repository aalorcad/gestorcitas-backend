package cl.duoc.gestorcitas.kafkaadmin.dto;

/** Broker del clúster. */
public record BrokerResponse(int id, String host, int puerto, String rack) {
}

package cl.duoc.gestorcitas.notificaciones.service;

import cl.duoc.gestorcitas.notificaciones.messaging.kafka.CitaEventoKafkaMsg;

/** Historial de estados de las citas, alimentado por el streaming de Kafka. */
public interface HistorialCitaService {

    /**
     * Agrega el evento al historial.
     *
     * @return false si el eventoId ya estaba registrado (entrega repetida)
     */
    boolean registrar(CitaEventoKafkaMsg evento, int particion, long offset);
}

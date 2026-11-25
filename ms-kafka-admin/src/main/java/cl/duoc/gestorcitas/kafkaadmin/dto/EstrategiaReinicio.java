package cl.duoc.gestorcitas.kafkaadmin.dto;

/** A dónde mover los offsets del grupo. */
public enum EstrategiaReinicio {
    /** Al primer registro disponible: el grupo reprocesa todo el historial retenido. */
    EARLIEST,
    /** Al final: el grupo ignora lo pendiente y solo lee registros nuevos. */
    LATEST,
    /** A un offset exacto (el mismo en todas las particiones; si supera el final se ajusta al final). */
    OFFSET
}

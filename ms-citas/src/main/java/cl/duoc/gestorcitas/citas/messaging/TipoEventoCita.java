package cl.duoc.gestorcitas.citas.messaging;

import cl.duoc.gestorcitas.citas.entity.EstadoCita;

/** Eventos del ciclo de vida de una cita que se publican en RabbitMQ. */
public enum TipoEventoCita {
    CREADA,
    CONFIRMADA,
    CANCELADA,
    ATENDIDA;

    /** Evento que corresponde a una transición hacia el estado indicado. */
    public static TipoEventoCita desdeEstado(EstadoCita estado) {
        return switch (estado) {
            case PENDIENTE -> CREADA;
            case CONFIRMADA -> CONFIRMADA;
            case CANCELADA -> CANCELADA;
            case ATENDIDA -> ATENDIDA;
        };
    }
}

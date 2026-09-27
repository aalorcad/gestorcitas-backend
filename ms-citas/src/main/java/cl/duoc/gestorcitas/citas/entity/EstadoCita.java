package cl.duoc.gestorcitas.citas.entity;

public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    CANCELADA,
    ATENDIDA;

    public boolean esActiva() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}

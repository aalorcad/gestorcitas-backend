package cl.duoc.gestorcitas.notificaciones.entity;

import cl.duoc.gestorcitas.notificaciones.messaging.kafka.CitaEventoKafkaMsg;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Historial de cambios de estado de cada cita, construido desde el streaming de Kafka (grupo "historial-citas").
 * {@code eventoId} es único: una entrega repetida no duplica la línea de historial.
 */
@Entity
@Table(name = "historial_citas",
        uniqueConstraints = @UniqueConstraint(name = "uk_historial_evento", columnNames = "evento_id"),
        indexes = @Index(name = "ix_historial_cita", columnList = "cita_id"))
public class HistorialCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, length = 64)
    private String eventoId;

    @Column(name = "cita_id", nullable = false)
    private Long citaId;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "estado", length = 20)
    private String estado;

    @Column(name = "medico_nombre", length = 150)
    private String medicoNombre;

    @Column(name = "especialidad_nombre", length = 150)
    private String especialidadNombre;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;

    @Column(name = "particion", nullable = false)
    private Integer particion;

    @Column(name = "offset_kafka", nullable = false)
    private Long offsetKafka;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    protected HistorialCita() {
        // requerido por JPA
    }

    public static HistorialCita desde(CitaEventoKafkaMsg e, int particion, long offset, LocalDateTime registradoEn) {
        HistorialCita h = new HistorialCita();
        h.eventoId = e.eventoId();
        h.citaId = e.citaId();
        h.tipo = e.tipo();
        h.estado = e.estado();
        h.medicoNombre = e.medicoNombre();
        h.especialidadNombre = e.especialidadNombre();
        h.fechaHora = e.fechaHora();
        h.ocurridoEn = e.ocurridoEn();
        h.particion = particion;
        h.offsetKafka = offset;
        h.registradoEn = registradoEn;
        return h;
    }

    public Long getId() { return id; }
    public String getEventoId() { return eventoId; }
    public Long getCitaId() { return citaId; }
    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public String getMedicoNombre() { return medicoNombre; }
    public String getEspecialidadNombre() { return especialidadNombre; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public Integer getParticion() { return particion; }
    public Long getOffsetKafka() { return offsetKafka; }
    public LocalDateTime getRegistradoEn() { return registradoEn; }
}

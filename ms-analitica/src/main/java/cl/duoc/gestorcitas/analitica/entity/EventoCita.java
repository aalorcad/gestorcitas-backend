package cl.duoc.gestorcitas.analitica.entity;

import cl.duoc.gestorcitas.analitica.messaging.event.CitaEventoMsg;
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
 * Evento de cita consumido desde Kafka. Es la base de los indicadores de analítica.
 * {@code eventoId} es único: un registro entregado dos veces (at-least-once) se cuenta una sola vez.
 * Se guarda también la partición y el offset de origen para poder rastrear cada fila hasta Kafka.
 */
@Entity
@Table(name = "eventos_cita",
        uniqueConstraints = @UniqueConstraint(name = "uk_eventos_cita_evento", columnNames = "evento_id"),
        indexes = @Index(name = "ix_eventos_cita_tipo", columnList = "tipo"))
public class EventoCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, length = 64)
    private String eventoId;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "cita_id", nullable = false)
    private Long citaId;

    @Column(name = "paciente_id", length = 100)
    private String pacienteId;

    @Column(name = "medico_id")
    private Long medicoId;

    @Column(name = "medico_nombre", length = 150)
    private String medicoNombre;

    @Column(name = "especialidad_id")
    private Long especialidadId;

    @Column(name = "especialidad_nombre", length = 150)
    private String especialidadNombre;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @Column(name = "estado", length = 20)
    private String estado;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;

    @Column(name = "particion", nullable = false)
    private Integer particion;

    @Column(name = "offset_kafka", nullable = false)
    private Long offsetKafka;

    @Column(name = "procesado_en", nullable = false)
    private LocalDateTime procesadoEn;

    protected EventoCita() {
        // requerido por JPA
    }

    public static EventoCita desde(CitaEventoMsg e, int particion, long offset, LocalDateTime procesadoEn) {
        EventoCita c = new EventoCita();
        c.eventoId = e.eventoId();
        c.tipo = e.tipo();
        c.citaId = e.citaId();
        c.pacienteId = e.pacienteId();
        c.medicoId = e.medicoId();
        c.medicoNombre = e.medicoNombre();
        c.especialidadId = e.especialidadId();
        c.especialidadNombre = e.especialidadNombre();
        c.fechaHora = e.fechaHora();
        c.estado = e.estado();
        c.ocurridoEn = e.ocurridoEn();
        c.particion = particion;
        c.offsetKafka = offset;
        c.procesadoEn = procesadoEn;
        return c;
    }

    public Long getId() { return id; }
    public String getEventoId() { return eventoId; }
    public String getTipo() { return tipo; }
    public Long getCitaId() { return citaId; }
    public String getPacienteId() { return pacienteId; }
    public Long getMedicoId() { return medicoId; }
    public String getMedicoNombre() { return medicoNombre; }
    public Long getEspecialidadId() { return especialidadId; }
    public String getEspecialidadNombre() { return especialidadNombre; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getEstado() { return estado; }
    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public Integer getParticion() { return particion; }
    public Long getOffsetKafka() { return offsetKafka; }
    public LocalDateTime getProcesadoEn() { return procesadoEn; }
}

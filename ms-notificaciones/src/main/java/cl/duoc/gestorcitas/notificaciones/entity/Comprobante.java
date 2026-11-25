package cl.duoc.gestorcitas.notificaciones.entity;

import cl.duoc.gestorcitas.notificaciones.messaging.event.SolicitudComprobante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Comprobante generado para una cita.
 * {@code solicitudId} es único: una solicitud repetida no genera un segundo comprobante.
 */
@Entity
@Table(name = "comprobantes",
        uniqueConstraints = @UniqueConstraint(name = "uk_comprobante_solicitud", columnNames = "solicitud_id"))
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false, length = 64)
    private String solicitudId;

    @Column(name = "codigo", nullable = false, length = 40)
    private String codigo;

    @Column(name = "cita_id", nullable = false)
    private Long citaId;

    @Column(name = "paciente_nombre", nullable = false, length = 120)
    private String pacienteNombre;

    @Column(name = "medico_nombre", nullable = false, length = 120)
    private String medicoNombre;

    @Column(name = "especialidad_nombre", length = 80)
    private String especialidadNombre;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "archivo", nullable = false, length = 255)
    private String archivo;

    @Column(name = "generado_en", nullable = false)
    private LocalDateTime generadoEn;

    protected Comprobante() {
        // requerido por JPA
    }

    public static Comprobante desde(SolicitudComprobante s, String codigo, String archivo, LocalDateTime generadoEn) {
        Comprobante c = new Comprobante();
        c.solicitudId = s.solicitudId();
        c.codigo = codigo;
        c.citaId = s.citaId();
        c.pacienteNombre = s.pacienteNombre();
        c.medicoNombre = s.medicoNombre();
        c.especialidadNombre = s.especialidadNombre();
        c.fechaHora = s.fechaHora();
        c.motivo = s.motivo();
        c.archivo = archivo;
        c.generadoEn = generadoEn;
        return c;
    }

    public Long getId() { return id; }
    public String getSolicitudId() { return solicitudId; }
    public String getCodigo() { return codigo; }
    public Long getCitaId() { return citaId; }
    public String getPacienteNombre() { return pacienteNombre; }
    public String getMedicoNombre() { return medicoNombre; }
    public String getEspecialidadNombre() { return especialidadNombre; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getMotivo() { return motivo; }
    public String getArchivo() { return archivo; }
    public LocalDateTime getGeneradoEn() { return generadoEn; }
}

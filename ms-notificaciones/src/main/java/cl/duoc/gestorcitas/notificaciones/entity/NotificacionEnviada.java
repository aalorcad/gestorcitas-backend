package cl.duoc.gestorcitas.notificaciones.entity;

import cl.duoc.gestorcitas.notificaciones.messaging.event.CitaEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Notificación enviada al paciente por un cambio en su cita.
 * {@code eventoId} es único: un evento repetido no genera un segundo correo.
 */
@Entity
@Table(name = "notificaciones_enviadas",
        uniqueConstraints = @UniqueConstraint(name = "uk_notificacion_evento", columnNames = "evento_id"))
public class NotificacionEnviada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, length = 64)
    private String eventoId;

    @Column(name = "tipo_evento", nullable = false, length = 30)
    private String tipoEvento;

    @Column(name = "cita_id", nullable = false)
    private Long citaId;

    @Column(name = "destinatario", nullable = false, length = 150)
    private String destinatario;

    @Column(name = "asunto", nullable = false, length = 150)
    private String asunto;

    @Column(name = "estado_cita", length = 20)
    private String estadoCita;

    @Column(name = "enviada_en", nullable = false)
    private LocalDateTime enviadaEn;

    protected NotificacionEnviada() {
        // requerido por JPA
    }

    public static NotificacionEnviada desde(CitaEvento e, String asunto, LocalDateTime enviadaEn) {
        NotificacionEnviada n = new NotificacionEnviada();
        n.eventoId = e.eventoId();
        n.tipoEvento = e.tipo();
        n.citaId = e.citaId();
        n.destinatario = e.pacienteEmail();
        n.asunto = asunto;
        n.estadoCita = e.estado();
        n.enviadaEn = enviadaEn;
        return n;
    }

    public Long getId() { return id; }
    public String getEventoId() { return eventoId; }
    public String getTipoEvento() { return tipoEvento; }
    public Long getCitaId() { return citaId; }
    public String getDestinatario() { return destinatario; }
    public String getAsunto() { return asunto; }
    public String getEstadoCita() { return estadoCita; }
    public LocalDateTime getEnviadaEn() { return enviadaEn; }
}

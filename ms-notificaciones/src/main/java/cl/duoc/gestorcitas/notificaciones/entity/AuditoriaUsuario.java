package cl.duoc.gestorcitas.notificaciones.entity;

import cl.duoc.gestorcitas.notificaciones.messaging.event.UsuarioEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Registro de auditoría de un cambio en el ciclo de vida de un usuario.
 * {@code eventoId} es único: si RabbitMQ entrega dos veces el mismo evento, no se duplica.
 */
@Entity
@Table(name = "auditoria_usuarios",
        uniqueConstraints = @UniqueConstraint(name = "uk_auditoria_evento", columnNames = "evento_id"))
public class AuditoriaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, length = 64)
    private String eventoId;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "nombre", length = 120)
    private String nombre;

    @Column(name = "roles", length = 100)
    private String roles;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "actor_id", length = 64)
    private String actorId;

    @Column(name = "ocurrido_en")
    private LocalDateTime ocurridoEn;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    protected AuditoriaUsuario() {
        // requerido por JPA
    }

    public static AuditoriaUsuario desde(UsuarioEvento e, LocalDateTime registradoEn) {
        AuditoriaUsuario a = new AuditoriaUsuario();
        a.eventoId = e.eventoId();
        a.tipo = e.tipo();
        a.usuarioId = e.usuarioId();
        a.email = e.email();
        a.nombre = e.nombre();
        a.roles = e.roles() == null ? null : String.join(",", e.roles());
        a.activo = e.activo();
        a.actorId = e.actorId();
        a.ocurridoEn = e.ocurridoEn();
        a.registradoEn = registradoEn;
        return a;
    }

    public Long getId() { return id; }
    public String getEventoId() { return eventoId; }
    public String getTipo() { return tipo; }
    public Long getUsuarioId() { return usuarioId; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public String getRoles() { return roles; }
    public boolean isActivo() { return activo; }
    public String getActorId() { return actorId; }
    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public LocalDateTime getRegistradoEn() { return registradoEn; }
}

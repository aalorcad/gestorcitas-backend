package cl.duoc.gestorcitas.analitica.entity;

import cl.duoc.gestorcitas.analitica.messaging.event.UsuarioEstadoMsg;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Último estado conocido de cada usuario: es la "tabla" que resulta de leer el tópico compactado
 * {@code gestorcitas.usuarios.estado}. La clave primaria es el mismo {@code usuarioId} que usa Kafka
 * como clave del registro, por lo que cada nuevo estado reemplaza al anterior.
 */
@Entity
@Table(name = "usuarios_estado")
public class UsuarioEstado {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "nombre", length = 150)
    private String nombre;

    /** Roles separados por coma, p. ej. "Medico,Paciente". */
    @Column(name = "roles", length = 100)
    private String roles;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "ultimo_tipo", nullable = false, length = 20)
    private String ultimoTipo;

    @Column(name = "ultimo_evento_id", nullable = false, length = 64)
    private String ultimoEventoId;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Column(name = "procesado_en", nullable = false)
    private LocalDateTime procesadoEn;

    protected UsuarioEstado() {
        // requerido por JPA
    }

    public static UsuarioEstado desde(UsuarioEstadoMsg e, LocalDateTime procesadoEn) {
        UsuarioEstado u = new UsuarioEstado();
        u.usuarioId = e.usuarioId();
        u.aplicar(e, procesadoEn);
        return u;
    }

    /** Reemplaza el estado guardado por el estado recibido. */
    public void aplicar(UsuarioEstadoMsg e, LocalDateTime procesadoEn) {
        this.email = e.email();
        this.nombre = e.nombre();
        this.roles = e.roles() == null ? "" : String.join(",", e.roles());
        this.activo = e.activo();
        this.ultimoTipo = e.tipo();
        this.ultimoEventoId = e.eventoId();
        this.actualizadoEn = e.actualizadoEn();
        this.procesadoEn = procesadoEn;
    }

    public List<String> listaDeRoles() {
        return roles == null || roles.isBlank() ? List.of() : List.of(roles.split(","));
    }

    public Long getUsuarioId() { return usuarioId; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public String getRoles() { return roles; }
    public boolean isActivo() { return activo; }
    public String getUltimoTipo() { return ultimoTipo; }
    public String getUltimoEventoId() { return ultimoEventoId; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public LocalDateTime getProcesadoEn() { return procesadoEn; }
}

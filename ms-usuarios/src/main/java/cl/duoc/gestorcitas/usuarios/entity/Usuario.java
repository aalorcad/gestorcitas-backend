package cl.duoc.gestorcitas.usuarios.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Object ID en Entra ID. Null para médicos pre-registrados que aún no inician sesión. */
    @Column(unique = true, length = 64)
    private String oid;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 20)
    @Builder.Default
    private Set<RolUsuario> roles = EnumSet.noneOf(RolUsuario.class);

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "perfil_paciente_id")
    private PerfilPaciente perfilPaciente;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "perfil_medico_id")
    private PerfilMedico perfilMedico;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    public boolean tieneRol(RolUsuario rol) {
        return roles != null && roles.contains(rol);
    }
}

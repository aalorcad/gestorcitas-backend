package cl.duoc.gestorcitas.usuarios.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "perfiles_paciente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** RUT normalizado: 12345678-9 */
    @Column(unique = true, length = 12)
    private String rut;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Prevision prevision;

    /** El paciente puede reservar solo con el perfil completo. */
    public boolean isCompleto() {
        return rut != null && fechaNacimiento != null && prevision != null;
    }
}

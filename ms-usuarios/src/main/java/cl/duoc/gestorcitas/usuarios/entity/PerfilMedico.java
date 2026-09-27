package cl.duoc.gestorcitas.usuarios.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "perfiles_medico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID de la especialidad en ms-catalogo. Null = pendiente de asignación por el Admin. */
    @Column(name = "especialidad_id")
    private Long especialidadId;

    @Column(name = "registro_profesional", length = 30)
    private String registroProfesional;

    @Column(length = 500)
    private String biografia;
}

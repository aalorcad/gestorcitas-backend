package cl.duoc.gestorcitas.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "especialidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    /** Valor de la consulta en pesos chilenos (CLP). */
    @Column(name = "valor_consulta", nullable = false)
    @Builder.Default
    private Integer valorConsulta = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;
}

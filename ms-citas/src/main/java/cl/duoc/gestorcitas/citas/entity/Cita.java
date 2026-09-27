package cl.duoc.gestorcitas.citas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "citas", indexes = {
        @Index(name = "idx_cita_medico_fecha", columnList = "medico_id, fecha_hora"),
        @Index(name = "idx_cita_paciente", columnList = "paciente_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Object ID (oid) del paciente en Microsoft Entra ID. */
    @Column(name = "paciente_id", nullable = false, length = 64)
    private String pacienteId;

    @Column(name = "paciente_nombre", nullable = false, length = 120)
    private String pacienteNombre;

    @Column(name = "paciente_email", length = 150)
    private String pacienteEmail;

    @Column(name = "medico_id", nullable = false)
    private Long medicoId;

    @Column(name = "medico_nombre", nullable = false, length = 120)
    private String medicoNombre;

    @Column(name = "medico_email", nullable = false, length = 150)
    private String medicoEmail;

    @Column(name = "especialidad_id", nullable = false)
    private Long especialidadId;

    @Column(name = "especialidad_nombre", nullable = false, length = 80)
    private String especialidadNombre;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(length = 255)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @Column(name = "actualizada_en")
    private LocalDateTime actualizadaEn;
}

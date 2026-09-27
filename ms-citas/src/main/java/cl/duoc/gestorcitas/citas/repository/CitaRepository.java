package cl.duoc.gestorcitas.citas.repository;

import cl.duoc.gestorcitas.citas.entity.Cita;
import cl.duoc.gestorcitas.citas.entity.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByPacienteIdOrderByFechaHoraDesc(String pacienteId);

    List<Cita> findByMedicoEmailIgnoreCaseAndFechaHoraBetweenOrderByFechaHoraAsc(
            String medicoEmail, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findByMedicoEmailIgnoreCaseOrderByFechaHoraAsc(String medicoEmail);

    List<Cita> findByMedicoIdAndFechaHoraBetweenAndEstadoIn(
            Long medicoId, LocalDateTime desde, LocalDateTime hasta, Collection<EstadoCita> estados);

    List<Cita> findAllByOrderByFechaHoraDesc();

    List<Cita> findByEstadoOrderByFechaHoraDesc(EstadoCita estado);

    long countByEstado(EstadoCita estado);

    long countByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);

    boolean existsByMedicoIdAndFechaHoraAndEstadoIn(Long medicoId, LocalDateTime fechaHora, Collection<EstadoCita> estados);

    boolean existsByPacienteIdAndFechaHoraAndEstadoIn(String pacienteId, LocalDateTime fechaHora, Collection<EstadoCita> estados);

    long countByPacienteIdAndEspecialidadIdAndFechaHoraAfterAndEstadoIn(
            String pacienteId, Long especialidadId, LocalDateTime fecha, Collection<EstadoCita> estados);
}

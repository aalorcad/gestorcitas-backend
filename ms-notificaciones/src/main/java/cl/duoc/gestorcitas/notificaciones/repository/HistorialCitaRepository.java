package cl.duoc.gestorcitas.notificaciones.repository;

import cl.duoc.gestorcitas.notificaciones.entity.HistorialCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialCitaRepository extends JpaRepository<HistorialCita, Long> {

    boolean existsByEventoId(String eventoId);

    List<HistorialCita> findByCitaIdOrderByOcurridoEnAsc(Long citaId);
}

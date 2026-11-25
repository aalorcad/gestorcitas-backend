package cl.duoc.gestorcitas.notificaciones.repository;

import cl.duoc.gestorcitas.notificaciones.entity.NotificacionEnviada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionEnviadaRepository extends JpaRepository<NotificacionEnviada, Long> {

    boolean existsByEventoId(String eventoId);
}

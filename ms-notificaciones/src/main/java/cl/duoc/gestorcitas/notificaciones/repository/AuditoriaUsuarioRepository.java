package cl.duoc.gestorcitas.notificaciones.repository;

import cl.duoc.gestorcitas.notificaciones.entity.AuditoriaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaUsuarioRepository extends JpaRepository<AuditoriaUsuario, Long> {

    boolean existsByEventoId(String eventoId);
}

package cl.duoc.gestorcitas.analitica.repository;

import cl.duoc.gestorcitas.analitica.entity.UsuarioEstado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioEstadoRepository extends JpaRepository<UsuarioEstado, Long> {

    long countByActivoTrue();
}

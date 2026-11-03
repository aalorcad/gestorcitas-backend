package cl.duoc.gestorcitas.catalogo.repository;

import cl.duoc.gestorcitas.catalogo.entity.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    List<Especialidad> findByActivaTrueOrderByNombreAsc();

    List<Especialidad> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}

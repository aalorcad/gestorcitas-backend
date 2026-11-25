package cl.duoc.gestorcitas.analitica.repository;

import cl.duoc.gestorcitas.analitica.entity.EventoCita;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventoCitaRepository extends JpaRepository<EventoCita, Long> {

    boolean existsByEventoId(String eventoId);

    long countByTipo(String tipo);

    @Query("select e.tipo as clave, count(e) as total from EventoCita e group by e.tipo order by count(e) desc")
    List<ConteoPorClave> contarPorTipo();

    @Query("""
            select coalesce(e.especialidadNombre, 'Sin especialidad') as clave, count(e) as total
            from EventoCita e where e.tipo = :tipo
            group by coalesce(e.especialidadNombre, 'Sin especialidad') order by count(e) desc""")
    List<ConteoPorClave> contarPorEspecialidad(@Param("tipo") String tipo);

    @Query("""
            select coalesce(e.medicoNombre, 'Sin médico') as clave, count(e) as total
            from EventoCita e where e.tipo = :tipo
            group by coalesce(e.medicoNombre, 'Sin médico') order by count(e) desc""")
    List<ConteoPorClave> contarPorMedico(@Param("tipo") String tipo);

    List<EventoCita> findAllByOrderByIdDesc(Pageable pagina);
}

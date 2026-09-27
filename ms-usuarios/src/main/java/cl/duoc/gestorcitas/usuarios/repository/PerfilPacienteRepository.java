package cl.duoc.gestorcitas.usuarios.repository;

import cl.duoc.gestorcitas.usuarios.entity.PerfilPaciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilPacienteRepository extends JpaRepository<PerfilPaciente, Long> {

    boolean existsByRut(String rut);

    boolean existsByRutAndIdNot(String rut, Long id);
}

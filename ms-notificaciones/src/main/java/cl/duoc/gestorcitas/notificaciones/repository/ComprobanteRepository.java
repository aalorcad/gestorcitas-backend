package cl.duoc.gestorcitas.notificaciones.repository;

import cl.duoc.gestorcitas.notificaciones.entity.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    Optional<Comprobante> findBySolicitudId(String solicitudId);
}

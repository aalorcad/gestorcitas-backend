package cl.duoc.gestorcitas.catalogo.config;

import cl.duoc.gestorcitas.catalogo.entity.Especialidad;
import cl.duoc.gestorcitas.catalogo.repository.EspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga especialidades de demostración si la base está vacía.
 * Los IDs 1, 2 y 3 son usados por los médicos pre-registrados en ms-usuarios.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final EspecialidadRepository especialidadRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (especialidadRepository.count() > 0) {
            return;
        }
        guardar("Medicina General", "Atención primaria y controles", 25000);
        guardar("Pediatría", "Atención de niños y adolescentes", 32000);
        guardar("Cardiología", "Enfermedades del corazón", 45000);
        guardar("Dermatología", "Enfermedades de la piel", 38000);
        log.info("Catálogo inicial de especialidades cargado");
    }

    private void guardar(String nombre, String descripcion, int valor) {
        especialidadRepository.save(Especialidad.builder()
                .nombre(nombre).descripcion(descripcion).valorConsulta(valor).activa(true).build());
    }
}

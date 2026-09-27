package cl.duoc.gestorcitas.usuarios.config;

import cl.duoc.gestorcitas.usuarios.entity.PerfilMedico;
import cl.duoc.gestorcitas.usuarios.entity.RolUsuario;
import cl.duoc.gestorcitas.usuarios.entity.Usuario;
import cl.duoc.gestorcitas.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Pre-registra médicos de demostración (sin oid). El primero usa DEMO_MEDICO_EMAIL para
 * que se vincule automáticamente con el usuario médico de prueba de Entra ID.
 * Los IDs de especialidad (1, 2, 3) corresponden a la carga inicial de ms-catalogo.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    @Value("${app.demo.medico-email:medico1@tu-tenant.onmicrosoft.com}")
    private String demoMedicoEmail;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        crearMedico("Dr. Elian Barra", demoMedicoEmail, 1L, "SIS-100245",
                "Médico general con enfoque en medicina preventiva y controles de salud.");
        crearMedico("Dr. Matías Soto", "msoto@clinica.demo", 2L, "SIS-200318",
                "Pediatra con 10 años de experiencia en atención infantil.");
        crearMedico("Dr. Felipe Muñoz", "fmunoz@clinica.demo", 3L, "SIS-300772",
                "Cardiólogo especialista en prevención cardiovascular.");
        log.info("Médicos de demostración pre-registrados (médico demo: {})", demoMedicoEmail);
    }

    private void crearMedico(String nombre, String email, Long especialidadId, String registro, String bio) {
        usuarioRepository.save(Usuario.builder()
                .nombre(nombre)
                .email(email.toLowerCase(Locale.ROOT))
                .activo(true)
                .roles(EnumSet.of(RolUsuario.MEDICO))
                .perfilMedico(PerfilMedico.builder()
                        .especialidadId(especialidadId)
                        .registroProfesional(registro)
                        .biografia(bio)
                        .build())
                .creadoEn(LocalDateTime.now(clock))
                .build());
    }
}

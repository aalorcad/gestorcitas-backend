package cl.duoc.gestorcitas.usuarios.service.impl;

import cl.duoc.gestorcitas.usuarios.dto.ActualizarPerfilPacienteRequest;
import cl.duoc.gestorcitas.usuarios.dto.UsuarioContexto;
import cl.duoc.gestorcitas.usuarios.dto.UsuarioResponse;
import cl.duoc.gestorcitas.usuarios.entity.*;
import cl.duoc.gestorcitas.usuarios.exception.BusinessException;
import cl.duoc.gestorcitas.usuarios.mapper.UsuarioMapper;
import cl.duoc.gestorcitas.usuarios.repository.PerfilPacienteRepository;
import cl.duoc.gestorcitas.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneId.of("America/Santiago"));

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilPacienteRepository perfilPacienteRepository;
    @Mock
    private EspecialidadResolver especialidades;

    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UsuarioServiceImpl(usuarioRepository, perfilPacienteRepository, new UsuarioMapper(), especialidades, CLOCK);
    }

    @Test
    void primerLoginDeMedicoPreRegistradoVinculaSuCuenta() {
        Usuario preRegistrado = Usuario.builder().id(7L).email("medico1@demo.cl").nombre("Dra. Rojas")
                .roles(EnumSet.of(RolUsuario.MEDICO))
                .perfilMedico(PerfilMedico.builder().especialidadId(1L).build())
                .creadoEn(LocalDateTime.now(CLOCK)).build();
        when(usuarioRepository.findByOid("oid-medico")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("medico1@demo.cl")).thenReturn(Optional.of(preRegistrado));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(especialidades.nombres()).thenReturn(Map.of(1L, "Medicina General"));

        UsuarioResponse r = service.sincronizar(new UsuarioContexto("oid-medico", "Camila Rojas", "Medico1@demo.cl", Set.of("Medico")));

        assertThat(r.id()).isEqualTo(7L);
        assertThat(r.oid()).isEqualTo("oid-medico");
        assertThat(r.vinculadoEntraId()).isTrue();
        assertThat(r.perfilMedico().especialidadNombre()).isEqualTo("Medicina General");
    }

    @Test
    void primerLoginDePacienteCreaPerfilIncompleto() {
        when(usuarioRepository.findByOid("oid-p")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("ana@demo.cl")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse r = service.sincronizar(new UsuarioContexto("oid-p", "Ana", "ana@demo.cl", Set.of("Paciente")));

        assertThat(r.roles()).containsExactly("Paciente");
        assertThat(r.perfilPaciente()).isNotNull();
        assertThat(r.perfilPaciente().completo()).isFalse();
    }

    @Test
    void rechazaRutInvalido() {
        Usuario paciente = Usuario.builder().id(1L).oid("oid-p").email("ana@demo.cl").nombre("Ana")
                .roles(EnumSet.of(RolUsuario.PACIENTE)).perfilPaciente(new PerfilPaciente()).activo(true).build();
        when(usuarioRepository.findByOid("oid-p")).thenReturn(Optional.of(paciente));

        var req = new ActualizarPerfilPacienteRequest("12345678-9", LocalDate.of(1990, 1, 1), Prevision.FONASA, null);
        assertThatThrownBy(() -> service.actualizarPerfilPaciente(req, new UsuarioContexto("oid-p", "Ana", "ana@demo.cl", Set.of("Paciente"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RUT");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void adminNoPuedeDesactivarseASiMismo() {
        Usuario admin = Usuario.builder().id(3L).oid("oid-admin").email("admin@demo.cl").nombre("Admin")
                .roles(EnumSet.of(RolUsuario.ADMIN)).activo(true).build();
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.desactivar(3L, new UsuarioContexto("oid-admin", "Admin", "admin@demo.cl", Set.of("Admin"))))
                .isInstanceOf(BusinessException.class);
    }
}

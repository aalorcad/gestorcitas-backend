package cl.duoc.gestorcitas.usuarios;

import cl.duoc.gestorcitas.usuarios.entity.RolUsuario;
import cl.duoc.gestorcitas.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Levanta el contexto con H2 y verifica la carga de médicos demo y las consultas JPQL. */
@SpringBootTest
class MsUsuariosApplicationTests {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void cargaMedicosDemoYConsultasFuncionan() {
        assertThat(usuarioRepository.findMedicosAgendables()).hasSize(3);
        assertThat(usuarioRepository.findMedicosAgendablesPorEspecialidad(2L)).hasSize(1);
        assertThat(usuarioRepository.findAllByOrderByNombreAsc()).hasSize(3);
        assertThat(usuarioRepository.listarPorRol(RolUsuario.MEDICO)).hasSize(3);
        assertThat(usuarioRepository.buscar("%soto%")).hasSize(1);
        assertThat(usuarioRepository.buscarPorRol(RolUsuario.MEDICO, "%felipe%")).hasSize(1);
    }
}

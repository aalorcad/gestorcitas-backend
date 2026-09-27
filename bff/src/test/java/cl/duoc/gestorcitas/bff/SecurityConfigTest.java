package cl.duoc.gestorcitas.bff;

import cl.duoc.gestorcitas.bff.service.CatalogoService;
import cl.duoc.gestorcitas.bff.service.CitaService;
import cl.duoc.gestorcitas.bff.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CitaService citaService;
    @MockitoBean
    private CatalogoService catalogoService;
    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/citas/mias")).andExpect(status().isUnauthorized());
    }

    @Test
    void pacientePuedeVerSusCitas() throws Exception {
        when(citaService.misCitas(any())).thenReturn(List.of());
        mvc.perform(get("/api/citas/mias")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Paciente"))))
                .andExpect(status().isOk());
    }

    @Test
    void pacienteNoPuedeVerTodasLasCitas() throws Exception {
        mvc.perform(get("/api/citas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Paciente"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void medicoPuedeVerSuAgenda() throws Exception {
        when(citaService.agenda(any(), any())).thenReturn(List.of());
        mvc.perform(get("/api/citas/agenda")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Medico"))))
                .andExpect(status().isOk());
    }

    @Test
    void pacientePuedeListarMedicosParaReservar() throws Exception {
        when(usuarioService.listarMedicos(any())).thenReturn(List.of());
        mvc.perform(get("/api/usuarios/medicos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Paciente"))))
                .andExpect(status().isOk());
    }

    @Test
    void pacienteNoPuedeAdministrarUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Paciente"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void medicoNoPuedeEditarPerfilDePaciente() throws Exception {
        mvc.perform(put("/api/usuarios/me/perfil-paciente")
                        .contentType("application/json")
                        .content("{\"rut\":\"11111111-1\",\"fechaNacimiento\":\"1990-01-01\",\"prevision\":\"FONASA\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Medico"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void preflightCorsPermitidoSinToken() throws Exception {
        mvc.perform(options("/api/citas/mias")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }
}

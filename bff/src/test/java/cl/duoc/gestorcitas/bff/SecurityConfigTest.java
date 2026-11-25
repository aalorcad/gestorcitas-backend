package cl.duoc.gestorcitas.bff;

import cl.duoc.gestorcitas.bff.service.CatalogoService;
import cl.duoc.gestorcitas.bff.service.CitaService;
import cl.duoc.gestorcitas.bff.service.AnaliticaService;
import cl.duoc.gestorcitas.bff.service.KafkaAdminService;
import cl.duoc.gestorcitas.bff.service.MensajeriaAdminService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
    @MockitoBean
    private MensajeriaAdminService mensajeriaAdminService;
    @MockitoBean
    private KafkaAdminService kafkaAdminService;
    @MockitoBean
    private AnaliticaService analiticaService;

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

    // ------------------------------------------------------------------ Administración de RabbitMQ (EP3)

    @Test
    void sinTokenNoSeAdministraRabbitMq() throws Exception {
        mvc.perform(get("/api/admin/mensajeria/queues")).andExpect(status().isUnauthorized());
    }

    @Test
    void pacienteNoPuedeAdministrarRabbitMq() throws Exception {
        mvc.perform(get("/api/admin/mensajeria/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Paciente"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeListarColas() throws Exception {
        when(mensajeriaAdminService.listarColas()).thenReturn(JsonNodeFactory.instance.arrayNode());
        mvc.perform(get("/api/admin/mensajeria/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ Kafka y analítica (EP5)

    @Test
    void sinTokenNoSeAdministraKafka() throws Exception {
        mvc.perform(get("/api/admin/kafka/topics")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/analitica/resumen")).andExpect(status().isUnauthorized());
    }

    @Test
    void medicoNoPuedeAdministrarKafkaNiVerAnalitica() throws Exception {
        mvc.perform(get("/api/admin/kafka/consumer-groups")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Medico"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/analitica/resumen")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Paciente"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeListarTopicosYVerAnalitica() throws Exception {
        when(kafkaAdminService.listarTopicos()).thenReturn(JsonNodeFactory.instance.arrayNode());
        when(analiticaService.resumen()).thenReturn(JsonNodeFactory.instance.objectNode());
        mvc.perform(get("/api/admin/kafka/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/analitica/resumen")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isOk());
    }
}

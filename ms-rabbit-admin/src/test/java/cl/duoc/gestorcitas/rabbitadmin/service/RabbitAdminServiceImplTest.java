package cl.duoc.gestorcitas.rabbitadmin.service;

import cl.duoc.gestorcitas.rabbitadmin.client.RabbitManagementClient;
import cl.duoc.gestorcitas.rabbitadmin.client.dto.ManagementBinding;
import cl.duoc.gestorcitas.rabbitadmin.client.dto.ManagementExchange;
import cl.duoc.gestorcitas.rabbitadmin.config.AdminProperties;
import cl.duoc.gestorcitas.rabbitadmin.dto.*;
import cl.duoc.gestorcitas.rabbitadmin.exception.ConflictoException;
import cl.duoc.gestorcitas.rabbitadmin.exception.RecursoNoEncontradoException;
import cl.duoc.gestorcitas.rabbitadmin.exception.SolicitudInvalidaException;
import cl.duoc.gestorcitas.rabbitadmin.service.impl.RabbitAdminServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RabbitAdminServiceImplTest {

    private final AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
    private final RabbitManagementClient management = mock(RabbitManagementClient.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        service = new RabbitAdminServiceImpl(amqpAdmin, management, rabbitTemplate, new ObjectMapper(),
                new AdminProperties(List.of("q.notificaciones.citas", "gestorcitas.citas.eventos")));
    }

    @Test
    void creaColaConDeadLetterYTtl() {
        when(amqpAdmin.getQueueInfo("q.demo"))
                .thenReturn(null)                                   // no existe antes de crearla
                .thenReturn(new QueueInformation("q.demo", 0, 0));  // existe después
        when(management.buscarExchange("gestorcitas.dlx"))
                .thenReturn(Optional.of(new ManagementExchange("gestorcitas.dlx", "direct", true, false)));

        ColaResponse r = service.crearCola(new CrearColaRequest("q.demo", true, false, 60_000, null,
                "gestorcitas.dlx", "q.demo.dlq"));

        ArgumentCaptor<Queue> cola = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(cola.capture());
        assertThat(cola.getValue().isDurable()).isTrue();
        assertThat(cola.getValue().getArguments())
                .containsEntry("x-message-ttl", 60_000)
                .containsEntry("x-dead-letter-exchange", "gestorcitas.dlx")
                .containsEntry("x-dead-letter-routing-key", "q.demo.dlq");
        assertThat(r.nombre()).isEqualTo("q.demo");
    }

    @Test
    void noCreaUnaColaQueYaExiste() {
        when(amqpAdmin.getQueueInfo("q.demo")).thenReturn(new QueueInformation("q.demo", 3, 1));

        assertThatThrownBy(() -> service.crearCola(new CrearColaRequest("q.demo", null, null, null, null, null, null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ya existe");
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void noCreaColaConDeadLetterExchangeInexistente() {
        when(management.buscarExchange("no.existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearCola(new CrearColaRequest("q.demo", null, null, null, null, "no.existe", null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarColaInexistenteDevuelve404() {
        when(amqpAdmin.getQueueInfo("q.fantasma")).thenReturn(null);

        assertThatThrownBy(() -> service.eliminarCola("q.fantasma", false, false))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void noEliminaColasProtegidasDelSistema() {
        assertThatThrownBy(() -> service.eliminarCola("q.notificaciones.citas", false, false))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("topología de GestorCitas");
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void respetaSoloSiVacia() {
        when(amqpAdmin.getQueueInfo("q.demo")).thenReturn(new QueueInformation("q.demo", 5, 0));

        assertThatThrownBy(() -> service.eliminarCola("q.demo", true, false))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("5 mensajes");
    }

    @Test
    void eliminaColaExistente() {
        when(amqpAdmin.getQueueInfo("q.demo")).thenReturn(new QueueInformation("q.demo", 0, 0));

        service.eliminarCola("q.demo", true, true);

        verify(amqpAdmin).deleteQueue("q.demo");
    }

    @Test
    void creaExchangeTopic() {
        when(management.buscarExchange("ex.demo")).thenReturn(Optional.empty());

        ExchangeResponse r = service.crearExchange(new CrearExchangeRequest("ex.demo", TipoExchange.TOPIC, null, null));

        ArgumentCaptor<Exchange> ex = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(ex.capture());
        assertThat(ex.getValue()).isInstanceOf(TopicExchange.class);
        assertThat(r.tipo()).isEqualTo("topic");
    }

    @Test
    void comodinesSoloEnExchangesTopic() {
        when(management.buscarExchange("ex.directo"))
                .thenReturn(Optional.of(new ManagementExchange("ex.directo", "direct", true, false)));
        when(amqpAdmin.getQueueInfo("q.demo")).thenReturn(new QueueInformation("q.demo", 0, 0));

        assertThatThrownBy(() -> service.crearBinding(new BindingRequest("ex.directo", "q.demo", "cita.*")))
                .isInstanceOf(SolicitudInvalidaException.class);
        verify(amqpAdmin, never()).declareBinding(any());
    }

    @Test
    void creaBindingCuandoExchangeYColaExisten() {
        when(management.buscarExchange("ex.demo"))
                .thenReturn(Optional.of(new ManagementExchange("ex.demo", "topic", true, false)));
        when(amqpAdmin.getQueueInfo("q.demo")).thenReturn(new QueueInformation("q.demo", 0, 0));
        when(management.listarBindings()).thenReturn(List.of());

        service.crearBinding(new BindingRequest("ex.demo", "q.demo", "cita.*"));

        ArgumentCaptor<Binding> b = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(b.capture());
        assertThat(b.getValue().getExchange()).isEqualTo("ex.demo");
        assertThat(b.getValue().getDestination()).isEqualTo("q.demo");
        assertThat(b.getValue().getRoutingKey()).isEqualTo("cita.*");
    }

    @Test
    void eliminarBindingInexistenteDevuelve404() {
        when(management.listarBindings())
                .thenReturn(List.of(new ManagementBinding("ex.demo", "q.demo", "queue", "otra.key")));

        assertThatThrownBy(() -> service.eliminarBinding(new BindingRequest("ex.demo", "q.demo", "cita.*")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(amqpAdmin, never()).removeBinding(any());
    }

    @Test
    void publicarInformaSiElMensajeEsEnrutable() {
        when(management.buscarExchange("gestorcitas.citas.eventos"))
                .thenReturn(Optional.of(new ManagementExchange("gestorcitas.citas.eventos", "topic", true, false)));
        when(management.listarBindings())
                .thenReturn(List.of(new ManagementBinding("gestorcitas.citas.eventos", "q.notificaciones.citas", "queue", "cita.*")));

        PublicacionResponse r = service.publicar(new PublicarMensajeRequest("gestorcitas.citas.eventos", "cita.creada",
                new ObjectMapper().createObjectNode().put("citaId", 1)));

        assertThat(r.enrutable()).isTrue();
        verify(rabbitTemplate).send(org.mockito.ArgumentMatchers.eq("gestorcitas.citas.eventos"),
                org.mockito.ArgumentMatchers.eq("cita.creada"), any(Message.class));
    }
}

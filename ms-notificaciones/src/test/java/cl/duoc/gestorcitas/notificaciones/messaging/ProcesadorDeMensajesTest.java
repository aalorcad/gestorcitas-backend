package cl.duoc.gestorcitas.notificaciones.messaging;

import cl.duoc.gestorcitas.notificaciones.exception.ErrorTransitorioException;
import cl.duoc.gestorcitas.notificaciones.exception.MensajeInvalidoException;
import cl.duoc.gestorcitas.notificaciones.messaging.config.MessagingProperties;
import cl.duoc.gestorcitas.notificaciones.messaging.config.MessagingProperties.Flujo;
import cl.duoc.gestorcitas.notificaciones.messaging.event.CitaEvento;
import cl.duoc.gestorcitas.notificaciones.messaging.support.ProcesadorDeMensajes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Verifica la política de ACK / reintento / DLQ que comparten todos los consumidores. */
class ProcesadorDeMensajesTest {

    private static final long TAG = 42L;
    private static final String HEADER = "x-gestorcitas-intento";
    private static final Flujo FLUJO = new Flujo("ex.citas", "cita.*", "q.citas", "q.citas.retry", "q.citas.dlq");
    private static final String EVENTO_VALIDO = """
            {"eventoId":"e-1","tipo":"CREADA","citaId":10,"pacienteNombre":"Ana",
             "pacienteEmail":"ana@demo.cl","medicoNombre":"Dra. Rojas","fechaHora":"2026-09-29T10:00:00"}
            """;

    private final Channel channel = mock(Channel.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final List<CitaEvento> procesados = new ArrayList<>();
    private ProcesadorDeMensajes procesador;

    @BeforeEach
    void setUp() {
        MessagingProperties props = new MessagingProperties("gestorcitas.dlx",
                new MessagingProperties.Reintentos(3, 5000, HEADER), FLUJO, FLUJO, FLUJO);
        procesador = new ProcesadorDeMensajes(new ObjectMapper().findAndRegisterModules(),
                Validation.buildDefaultValidatorFactory().getValidator(), rabbitTemplate, props);
    }

    @Test
    void mensajeValidoSeProcesaYConfirmaConAck() throws Exception {
        procesador.procesar(mensaje(EVENTO_VALIDO, null), channel, FLUJO, CitaEvento.class, procesados::add);

        assertThat(procesados).hasSize(1);
        assertThat(procesados.get(0).citaId()).isEqualTo(10L);
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void jsonInvalidoVaDirectoALaDlqSinReintentos() throws Exception {
        procesador.procesar(mensaje("{esto no es json", null), channel, FLUJO, CitaEvento.class, procesados::add);

        assertThat(procesados).isEmpty();
        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void camposObligatoriosFaltantesVanALaDlq() throws Exception {
        String sinEmail = "{\"eventoId\":\"e-2\",\"tipo\":\"CREADA\",\"citaId\":10}";

        procesador.procesar(mensaje(sinEmail, null), channel, FLUJO, CitaEvento.class, procesados::add);

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void errorNoRecuperableDelNegocioVaALaDlq() throws Exception {
        Consumer<CitaEvento> falla = e -> { throw new MensajeInvalidoException("tipo desconocido"); };

        procesador.procesar(mensaje(EVENTO_VALIDO, null), channel, FLUJO, CitaEvento.class, falla);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void errorRecuperableSeReprogramaEnLaColaDeReintentoConIntentoSiguiente() throws Exception {
        Consumer<CitaEvento> falla = e -> { throw new ErrorTransitorioException("SMTP caído"); };

        procesador.procesar(mensaje(EVENTO_VALIDO, null), channel, FLUJO, CitaEvento.class, falla);

        ArgumentCaptor<Message> copia = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(""), eq("q.citas.retry"), copia.capture());
        assertThat(copia.getValue().getMessageProperties().<Integer>getHeader(HEADER)).isEqualTo(2);
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void alAgotarLosReintentosElMensajeVaALaDlq() throws Exception {
        Consumer<CitaEvento> falla = e -> { throw new ErrorTransitorioException("SMTP caído"); };

        procesador.procesar(mensaje(EVENTO_VALIDO, 3), channel, FLUJO, CitaEvento.class, falla);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(rabbitTemplate);
    }

    private static Message mensaje(String json, Integer intento) {
        MessageProperties mp = new MessageProperties();
        mp.setDeliveryTag(TAG);
        mp.setMessageId("m-1");
        mp.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        if (intento != null) {
            mp.setHeader(HEADER, intento);
        }
        return new Message(json.getBytes(StandardCharsets.UTF_8), mp);
    }
}

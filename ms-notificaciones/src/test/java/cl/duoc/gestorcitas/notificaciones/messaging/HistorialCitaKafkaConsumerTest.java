package cl.duoc.gestorcitas.notificaciones.messaging;

import cl.duoc.gestorcitas.notificaciones.entity.HistorialCita;
import cl.duoc.gestorcitas.notificaciones.exception.MensajeInvalidoException;
import cl.duoc.gestorcitas.notificaciones.messaging.kafka.CitaEventoKafkaMsg;
import cl.duoc.gestorcitas.notificaciones.messaging.kafka.HistorialCitaKafkaConsumer;
import cl.duoc.gestorcitas.notificaciones.repository.HistorialCitaRepository;
import cl.duoc.gestorcitas.notificaciones.service.impl.HistorialCitaServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Consumidor Kafka "historial-citas": guarda el evento con partición/offset, descarta duplicados y rechaza inválidos. */
class HistorialCitaKafkaConsumerTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final HistorialCitaRepository repositorio = mock(HistorialCitaRepository.class);
    private final HistorialCitaKafkaConsumer consumidor =
            new HistorialCitaKafkaConsumer(new HistorialCitaServiceImpl(repositorio), VALIDATOR);

    private static CitaEventoKafkaMsg evento(String eventoId, Long citaId) {
        return new CitaEventoKafkaMsg(eventoId, "CONFIRMADA", citaId, "oid", 3L, "Dra. Rojas", 10L,
                "Medicina General", LocalDateTime.of(2026, 9, 29, 10, 0), "CONFIRMADA", LocalDateTime.of(2026, 9, 28, 9, 0));
    }

    private static ConsumerRecord<String, CitaEventoKafkaMsg> registro(CitaEventoKafkaMsg valor) {
        return new ConsumerRecord<>("gestorcitas.citas.eventos", 2, 15L, "42", valor);
    }

    @Test
    void guardaElEventoConSuParticionYOffset() {
        consumidor.consumir(registro(evento("e-1", 42L)));

        ArgumentCaptor<HistorialCita> guardado = ArgumentCaptor.forClass(HistorialCita.class);
        verify(repositorio).save(guardado.capture());
        assertThat(guardado.getValue().getCitaId()).isEqualTo(42L);
        assertThat(guardado.getValue().getTipo()).isEqualTo("CONFIRMADA");
        assertThat(guardado.getValue().getParticion()).isEqualTo(2);
        assertThat(guardado.getValue().getOffsetKafka()).isEqualTo(15L);
    }

    @Test
    void eventoRepetidoNoSeGuardaDosVeces() {
        when(repositorio.existsByEventoId("e-1")).thenReturn(true);

        consumidor.consumir(registro(evento("e-1", 42L)));

        verify(repositorio, never()).save(any());
    }

    @Test
    void eventoSinCitaIdEsInvalidoYNoSeReintenta() {
        assertThatThrownBy(() -> consumidor.consumir(registro(evento("e-2", null))))
                .isInstanceOf(MensajeInvalidoException.class)
                .hasMessageContaining("citaId");

        verifyNoInteractions(repositorio);
    }
}

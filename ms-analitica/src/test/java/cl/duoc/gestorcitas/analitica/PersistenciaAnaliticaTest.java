package cl.duoc.gestorcitas.analitica;

import cl.duoc.gestorcitas.analitica.entity.EventoCita;
import cl.duoc.gestorcitas.analitica.messaging.event.CitaEventoMsg;
import cl.duoc.gestorcitas.analitica.repository.ConteoPorClave;
import cl.duoc.gestorcitas.analitica.repository.EventoCitaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Consultas agrupadas de analítica y restricción de unicidad de eventoId (H2 en modo Oracle). */
@DataJpaTest
class PersistenciaAnaliticaTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 9, 28, 8, 0);

    @Autowired
    private EventoCitaRepository repositorio;

    private EventoCita evento(String id, String tipo, String especialidad, long offset) {
        return EventoCita.desde(new CitaEventoMsg(id, tipo, offset, "oid", 3L, "Dra. Rojas", 10L, especialidad,
                T0, "PENDIENTE", T0), 0, offset, T0);
    }

    @Test
    void agrupaPorTipoYEspecialidad() {
        repositorio.saveAllAndFlush(List.of(
                evento("a", "CREADA", "Medicina General", 1),
                evento("b", "CREADA", "Medicina General", 2),
                evento("c", "CREADA", "Pediatría", 3),
                evento("d", "CANCELADA", "Pediatría", 4)));

        assertThat(repositorio.contarPorTipo())
                .extracting(ConteoPorClave::getClave, ConteoPorClave::getTotal)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("CREADA", 3L),
                        org.assertj.core.groups.Tuple.tuple("CANCELADA", 1L));
        assertThat(repositorio.contarPorEspecialidad("CREADA"))
                .extracting(ConteoPorClave::getClave, ConteoPorClave::getTotal)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Medicina General", 2L),
                        org.assertj.core.groups.Tuple.tuple("Pediatría", 1L));
        assertThat(repositorio.countByTipo("CANCELADA")).isEqualTo(1);
        assertThat(repositorio.findAllByOrderByIdDesc(PageRequest.of(0, 2)))
                .extracting(EventoCita::getEventoId).containsExactly("d", "c");
    }

    @Test
    void elMismoEventoIdNoSePuedeGuardarDosVeces() {
        repositorio.saveAndFlush(evento("x", "CREADA", "Medicina General", 1));

        assertThat(repositorio.existsByEventoId("x")).isTrue();
        assertThatThrownBy(() -> repositorio.saveAndFlush(evento("x", "CREADA", "Medicina General", 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

package cl.duoc.gestorcitas.analitica.service.impl;

import cl.duoc.gestorcitas.analitica.dto.ConteoResponse;
import cl.duoc.gestorcitas.analitica.dto.EventoCitaResponse;
import cl.duoc.gestorcitas.analitica.dto.ResumenAnaliticaResponse;
import cl.duoc.gestorcitas.analitica.entity.EventoCita;
import cl.duoc.gestorcitas.analitica.entity.UsuarioEstado;
import cl.duoc.gestorcitas.analitica.messaging.event.CitaEventoMsg;
import cl.duoc.gestorcitas.analitica.messaging.event.UsuarioEstadoMsg;
import cl.duoc.gestorcitas.analitica.repository.ConteoPorClave;
import cl.duoc.gestorcitas.analitica.repository.EventoCitaRepository;
import cl.duoc.gestorcitas.analitica.repository.UsuarioEstadoRepository;
import cl.duoc.gestorcitas.analitica.service.AnaliticaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class AnaliticaServiceImpl implements AnaliticaService {

    static final String CREADA = "CREADA";
    static final String CANCELADA = "CANCELADA";
    static final int LIMITE_MAXIMO = 100;

    private final EventoCitaRepository eventos;
    private final UsuarioEstadoRepository usuarios;
    private final Clock clock;

    public AnaliticaServiceImpl(EventoCitaRepository eventos, UsuarioEstadoRepository usuarios, Clock clock) {
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ Escritura (consumidores Kafka)

    @Override
    @Transactional
    public Resultado registrarEventoCita(CitaEventoMsg evento, int particion, long offset) {
        if (eventos.existsByEventoId(evento.eventoId())) {
            return Resultado.DUPLICADO;
        }
        eventos.save(EventoCita.desde(evento, particion, offset, LocalDateTime.now(clock)));
        return Resultado.REGISTRADO;
    }

    @Override
    @Transactional
    public Resultado aplicarEstadoUsuario(UsuarioEstadoMsg estado) {
        Optional<UsuarioEstado> guardado = usuarios.findById(estado.usuarioId());
        if (guardado.isEmpty()) {
            usuarios.save(UsuarioEstado.desde(estado, LocalDateTime.now(clock)));
            return Resultado.REGISTRADO;
        }
        UsuarioEstado actual = guardado.get();
        if (actual.getUltimoEventoId().equals(estado.eventoId())) {
            return Resultado.DUPLICADO;
        }
        if (estado.actualizadoEn().isBefore(actual.getActualizadoEn())) {
            return Resultado.IGNORADO_ANTIGUO;
        }
        actual.aplicar(estado, LocalDateTime.now(clock));
        return Resultado.ACTUALIZADO;
    }

    @Override
    @Transactional
    public Resultado eliminarUsuario(Long usuarioId) {
        if (!usuarios.existsById(usuarioId)) {
            return Resultado.SIN_CAMBIOS;
        }
        usuarios.deleteById(usuarioId);
        return Resultado.ELIMINADO;
    }

    // ------------------------------------------------------------------ Lectura (API REST)

    @Override
    @Transactional(readOnly = true)
    public ResumenAnaliticaResponse resumen() {
        Map<String, Long> porTipo = new LinkedHashMap<>();
        for (ConteoPorClave c : eventos.contarPorTipo()) {
            porTipo.put(c.getClave(), c.getTotal());
        }
        long total = porTipo.values().stream().mapToLong(Long::longValue).sum();
        long creadas = porTipo.getOrDefault(CREADA, 0L);
        long canceladas = porTipo.getOrDefault(CANCELADA, 0L);

        return new ResumenAnaliticaResponse(
                total,
                porTipo,
                aRespuesta(eventos.contarPorEspecialidad(CREADA)),
                aRespuesta(eventos.contarPorMedico(CREADA)),
                porcentaje(canceladas, creadas),
                resumenUsuarios(),
                LocalDateTime.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoCitaResponse> ultimosEventos(int limite) {
        int tamano = Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        return eventos.findAllByOrderByIdDesc(PageRequest.of(0, tamano)).stream()
                .map(EventoCitaResponse::desde)
                .toList();
    }

    private ResumenAnaliticaResponse.UsuariosResumen resumenUsuarios() {
        List<UsuarioEstado> todos = usuarios.findAll();
        long activos = todos.stream().filter(UsuarioEstado::isActivo).count();
        Map<String, Long> porRol = new TreeMap<>();
        todos.forEach(u -> u.listaDeRoles().forEach(rol -> porRol.merge(rol, 1L, Long::sum)));
        return new ResumenAnaliticaResponse.UsuariosResumen(todos.size(), activos, todos.size() - activos, porRol);
    }

    private static List<ConteoResponse> aRespuesta(List<ConteoPorClave> conteos) {
        return conteos.stream().map(c -> new ConteoResponse(c.getClave(), c.getTotal())).toList();
    }

    /** Porcentaje con 1 decimal; 0 cuando no hay base. */
    static double porcentaje(long parte, long base) {
        if (base == 0) {
            return 0.0;
        }
        return Math.round(parte * 1000.0 / base) / 10.0;
    }
}

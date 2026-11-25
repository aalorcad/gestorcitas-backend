package cl.duoc.gestorcitas.notificaciones.service.impl;

import cl.duoc.gestorcitas.notificaciones.config.ComprobantesProperties;
import cl.duoc.gestorcitas.notificaciones.entity.Comprobante;
import cl.duoc.gestorcitas.notificaciones.exception.ErrorTransitorioException;
import cl.duoc.gestorcitas.notificaciones.messaging.event.SolicitudComprobante;
import cl.duoc.gestorcitas.notificaciones.repository.ComprobanteRepository;
import cl.duoc.gestorcitas.notificaciones.service.ComprobanteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Genera el comprobante de la reserva como archivo de texto y lo registra en Oracle (tabla COMPROBANTES).
 * Es idempotente: si la misma solicitud llega dos veces, se devuelve el comprobante ya generado.
 */
@Service
public class ComprobanteServiceImpl implements ComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteServiceImpl.class);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_CODIGO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ComprobantesProperties props;
    private final ComprobanteRepository repository;

    public ComprobanteServiceImpl(ComprobantesProperties props, ComprobanteRepository repository) {
        this.props = props;
        this.repository = repository;
    }

    @Override
    @Transactional
    public Path generar(SolicitudComprobante s) {
        Optional<Comprobante> existente = repository.findBySolicitudId(s.solicitudId());
        if (existente.isPresent()) {
            log.info("[DUPLICADO] La solicitud {} ya tiene el comprobante {}; se ignora",
                    s.solicitudId(), existente.get().getCodigo());
            return Path.of(existente.get().getArchivo());
        }

        String codigo = "GC-" + s.fechaHora().format(FORMATO_CODIGO) + "-" + s.citaId();
        String contenido = String.join(System.lineSeparator(),
                "=========================================",
                "        GESTORCITAS - COMPROBANTE",
                "=========================================",
                "Código:        " + codigo,
                "Cita N°:       " + s.citaId(),
                "Paciente:      " + s.pacienteNombre(),
                "Médico:        " + s.medicoNombre(),
                "Especialidad:  " + valor(s.especialidadNombre()),
                "Fecha y hora:  " + s.fechaHora().format(FORMATO),
                "Motivo:        " + valor(s.motivo()),
                "Solicitud:     " + s.solicitudId(),
                "=========================================",
                "Preséntese 15 minutos antes de su hora.");
        Path archivo;
        try {
            Path directorio = Path.of(props.directorio());
            Files.createDirectories(directorio);
            archivo = directorio.resolve("comprobante-" + codigo + ".txt");
            Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // Disco lleno o sin permisos: error temporal, se reintenta
            throw new ErrorTransitorioException("No se pudo escribir el comprobante: " + ex.getMessage(), ex);
        }
        repository.save(Comprobante.desde(s, codigo, archivo.toString(), LocalDateTime.now()));
        log.info("[COMPROBANTE] Generado {} para la cita #{} ({})", archivo, s.citaId(), s.pacienteNombre());
        return archivo;
    }

    private static String valor(String v) {
        return v == null || v.isBlank() ? "-" : v;
    }
}

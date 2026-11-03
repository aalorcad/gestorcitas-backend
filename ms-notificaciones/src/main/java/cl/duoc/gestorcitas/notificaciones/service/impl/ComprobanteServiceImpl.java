package cl.duoc.gestorcitas.notificaciones.service.impl;

import cl.duoc.gestorcitas.notificaciones.config.ComprobantesProperties;
import cl.duoc.gestorcitas.notificaciones.exception.ErrorTransitorioException;
import cl.duoc.gestorcitas.notificaciones.messaging.event.SolicitudComprobante;
import cl.duoc.gestorcitas.notificaciones.service.ComprobanteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * Genera el comprobante de la reserva como archivo de texto.
 * Es idempotente: si el mensaje se procesa dos veces, el archivo se sobrescribe con el mismo contenido.
 */
@Service
public class ComprobanteServiceImpl implements ComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteServiceImpl.class);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_CODIGO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ComprobantesProperties props;

    public ComprobanteServiceImpl(ComprobantesProperties props) {
        this.props = props;
    }

    @Override
    public Path generar(SolicitudComprobante s) {
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
        try {
            Path directorio = Path.of(props.directorio());
            Files.createDirectories(directorio);
            Path archivo = directorio.resolve("comprobante-" + codigo + ".txt");
            Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
            log.info("[COMPROBANTE] Generado {} para la cita #{} ({})", archivo, s.citaId(), s.pacienteNombre());
            return archivo;
        } catch (IOException ex) {
            // Disco lleno o sin permisos: error temporal, se reintenta
            throw new ErrorTransitorioException("No se pudo escribir el comprobante: " + ex.getMessage(), ex);
        }
    }

    private static String valor(String v) {
        return v == null || v.isBlank() ? "-" : v;
    }
}

package cl.duoc.gestorcitas.notificaciones.service.impl;

import cl.duoc.gestorcitas.notificaciones.config.SimulacionProperties;
import cl.duoc.gestorcitas.notificaciones.entity.NotificacionEnviada;
import cl.duoc.gestorcitas.notificaciones.exception.ErrorTransitorioException;
import cl.duoc.gestorcitas.notificaciones.exception.MensajeInvalidoException;
import cl.duoc.gestorcitas.notificaciones.messaging.event.CitaEvento;
import cl.duoc.gestorcitas.notificaciones.repository.NotificacionEnviadaRepository;
import cl.duoc.gestorcitas.notificaciones.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Notificación por correo al paciente. El envío real se simula registrando el correo en el log
 * (el proyecto no cuenta con un servidor SMTP), pero la lógica decide asunto y cuerpo según el evento.
 * Cada notificación enviada queda guardada en Oracle (tabla NOTIFICACIONES_ENVIADAS); un evento
 * repetido no genera un segundo correo.
 */
@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionServiceImpl.class);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final SimulacionProperties simulacion;
    private final NotificacionEnviadaRepository repository;

    public NotificacionServiceImpl(SimulacionProperties simulacion, NotificacionEnviadaRepository repository) {
        this.simulacion = simulacion;
        this.repository = repository;
    }

    @Override
    @Transactional
    public void notificarCambioDeCita(CitaEvento evento) {
        String asunto = asunto(evento.tipo());
        if (repository.existsByEventoId(evento.eventoId())) {
            log.info("[DUPLICADO] La notificación del evento {} (cita #{}) ya fue enviada; se ignora",
                    evento.eventoId(), evento.citaId());
            return;
        }
        if (simulacion.correoCaidoPara(evento.pacienteEmail())) {
            throw new ErrorTransitorioException("Servidor de correo no disponible para " + evento.pacienteEmail());
        }
        repository.save(NotificacionEnviada.desde(evento, asunto, LocalDateTime.now()));
        log.info("[CORREO] Para: {} | Asunto: {} | Hola {}, tu cita #{} con {} ({}) del {} quedó en estado {}.",
                evento.pacienteEmail(), asunto, evento.pacienteNombre(), evento.citaId(), evento.medicoNombre(),
                evento.especialidadNombre(), evento.fechaHora().format(FORMATO), evento.estado());
    }

    private static String asunto(String tipo) {
        return switch (tipo) {
            case "CREADA" -> "Recibimos tu reserva";
            case "CONFIRMADA" -> "Tu cita fue confirmada";
            case "CANCELADA" -> "Tu cita fue cancelada";
            case "ATENDIDA" -> "Gracias por atenderte con nosotros";
            default -> throw new MensajeInvalidoException("Tipo de evento de cita desconocido: " + tipo);
        };
    }
}

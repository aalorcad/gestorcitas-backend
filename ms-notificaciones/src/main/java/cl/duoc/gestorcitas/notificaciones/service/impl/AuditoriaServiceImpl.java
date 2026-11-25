package cl.duoc.gestorcitas.notificaciones.service.impl;

import cl.duoc.gestorcitas.notificaciones.entity.AuditoriaUsuario;
import cl.duoc.gestorcitas.notificaciones.messaging.event.UsuarioEvento;
import cl.duoc.gestorcitas.notificaciones.repository.AuditoriaUsuarioRepository;
import cl.duoc.gestorcitas.notificaciones.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Bitácora de auditoría de usuarios: se guarda en Oracle (tabla AUDITORIA_USUARIOS) y además se
 * escribe en un logger propio ("AUDITORIA"). Es idempotente por {@code eventoId}.
 */
@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA");

    private final AuditoriaUsuarioRepository repository;

    public AuditoriaServiceImpl(AuditoriaUsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void registrar(UsuarioEvento e) {
        if (repository.existsByEventoId(e.eventoId())) {
            auditoria.info("[DUPLICADO] Evento de usuario {} ya registrado; se ignora", e.eventoId());
            return;
        }
        repository.save(AuditoriaUsuario.desde(e, LocalDateTime.now()));
        auditoria.info("[AUDITORIA] evento={} usuarioId={} email={} nombre={} roles={} activo={} actor={} fecha={} id={}",
                e.tipo(), e.usuarioId(), e.email(), e.nombre(), e.roles(), e.activo(), e.actorId(),
                e.ocurridoEn(), e.eventoId());
    }
}

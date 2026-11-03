package cl.duoc.gestorcitas.notificaciones.service.impl;

import cl.duoc.gestorcitas.notificaciones.messaging.event.UsuarioEvento;
import cl.duoc.gestorcitas.notificaciones.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Bitácora de auditoría de usuarios: usa un logger propio ("AUDITORIA") para poder separarlo. */
@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA");

    @Override
    public void registrar(UsuarioEvento e) {
        auditoria.info("[AUDITORIA] evento={} usuarioId={} email={} nombre={} roles={} activo={} actor={} fecha={} id={}",
                e.tipo(), e.usuarioId(), e.email(), e.nombre(), e.roles(), e.activo(), e.actorId(),
                e.ocurridoEn(), e.eventoId());
    }
}

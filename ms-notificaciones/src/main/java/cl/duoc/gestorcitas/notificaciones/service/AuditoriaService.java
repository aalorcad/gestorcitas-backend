package cl.duoc.gestorcitas.notificaciones.service;

import cl.duoc.gestorcitas.notificaciones.messaging.event.UsuarioEvento;

/** Registra en la bitácora de auditoría los cambios del ciclo de vida de los usuarios. */
public interface AuditoriaService {

    void registrar(UsuarioEvento evento);
}

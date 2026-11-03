package cl.duoc.gestorcitas.notificaciones.service;

import cl.duoc.gestorcitas.notificaciones.messaging.event.CitaEvento;

/** Envía al paciente la notificación correspondiente a un cambio en su cita. */
public interface NotificacionService {

    void notificarCambioDeCita(CitaEvento evento);
}

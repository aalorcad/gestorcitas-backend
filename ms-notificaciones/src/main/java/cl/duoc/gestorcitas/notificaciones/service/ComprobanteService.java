package cl.duoc.gestorcitas.notificaciones.service;

import cl.duoc.gestorcitas.notificaciones.messaging.event.SolicitudComprobante;

import java.nio.file.Path;

/** Genera el comprobante (ticket) de una cita reservada. */
public interface ComprobanteService {

    /** @return ruta del comprobante generado */
    Path generar(SolicitudComprobante solicitud);
}

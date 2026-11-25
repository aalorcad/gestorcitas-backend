package cl.duoc.gestorcitas.analitica.service;

import cl.duoc.gestorcitas.analitica.dto.EventoCitaResponse;
import cl.duoc.gestorcitas.analitica.dto.ResumenAnaliticaResponse;
import cl.duoc.gestorcitas.analitica.messaging.event.CitaEventoMsg;
import cl.duoc.gestorcitas.analitica.messaging.event.UsuarioEstadoMsg;

import java.util.List;

/** Procesamiento del streaming (escritura) e indicadores (lectura) de analítica. */
public interface AnaliticaService {

    /** Resultado de aplicar un registro de Kafka; los consumidores lo usan para su log. */
    enum Resultado {
        /** Fila nueva guardada. */
        REGISTRADO,
        /** Estado de usuario existente reemplazado por uno más reciente. */
        ACTUALIZADO,
        /** El eventoId ya se había procesado (entrega repetida): no se cuenta dos veces. */
        DUPLICADO,
        /** El estado recibido es más antiguo que el guardado: se ignora. */
        IGNORADO_ANTIGUO,
        /** Tombstone (valor nulo) del tópico compactado: el usuario se elimina. */
        ELIMINADO,
        /** Tombstone de un usuario que no existía. */
        SIN_CAMBIOS
    }

    Resultado registrarEventoCita(CitaEventoMsg evento, int particion, long offset);

    Resultado aplicarEstadoUsuario(UsuarioEstadoMsg estado);

    Resultado eliminarUsuario(Long usuarioId);

    ResumenAnaliticaResponse resumen();

    List<EventoCitaResponse> ultimosEventos(int limite);
}

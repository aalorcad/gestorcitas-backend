package cl.duoc.gestorcitas.analitica.exception;

/**
 * El registro de Kafka no cumple el contrato (campos obligatorios ausentes o valores no permitidos).
 * Es un error PERMANENTE: reintentar no lo corrige, por eso va directo al tópico .DLT sin reintentos.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}

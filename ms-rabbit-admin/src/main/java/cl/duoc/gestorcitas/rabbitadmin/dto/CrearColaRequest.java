package cl.duoc.gestorcitas.rabbitadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Petición para crear una cola.
 *
 * @param nombre               nombre de la cola (obligatorio)
 * @param durable              sobrevive a reinicios del broker (por defecto true)
 * @param autoDelete           se elimina cuando se desconecta el último consumidor (por defecto false)
 * @param ttlMs                tiempo de vida de cada mensaje en milisegundos (opcional)
 * @param maxMensajes          largo máximo de la cola (opcional)
 * @param deadLetterExchange   exchange al que se envían los mensajes rechazados o expirados (opcional)
 * @param deadLetterRoutingKey routing key para el dead letter exchange (requiere deadLetterExchange)
 */
public record CrearColaRequest(
        @NombreAmqp String nombre,
        Boolean durable,
        Boolean autoDelete,
        @Positive(message = "debe ser mayor que 0")
        @Max(value = 604_800_000, message = "no puede superar 7 días (604800000 ms)")
        Integer ttlMs,
        @Positive(message = "debe ser mayor que 0")
        @Max(value = 1_000_000, message = "no puede superar 1.000.000 mensajes")
        Integer maxMensajes,
        @Size(max = ReglasAmqp.LARGO_MAXIMO, message = "no puede superar " + ReglasAmqp.LARGO_MAXIMO + " caracteres")
        @Pattern(regexp = ReglasAmqp.PATRON_NOMBRE, message = ReglasAmqp.MENSAJE_NOMBRE)
        String deadLetterExchange,
        @Size(max = ReglasAmqp.LARGO_MAXIMO, message = "no puede superar " + ReglasAmqp.LARGO_MAXIMO + " caracteres")
        @Pattern(regexp = ReglasAmqp.PATRON_ROUTING_KEY, message = ReglasAmqp.MENSAJE_ROUTING_KEY)
        String deadLetterRoutingKey
) {

    public boolean esDurable() {
        return durable == null || durable;
    }

    public boolean esAutoDelete() {
        return autoDelete != null && autoDelete;
    }

    public boolean tieneDeadLetter() {
        return deadLetterExchange != null && !deadLetterExchange.isBlank();
    }

    /** Regla cruzada: no tiene sentido una routing key de dead letter sin dead letter exchange. */
    @AssertTrue(message = "deadLetterRoutingKey requiere indicar deadLetterExchange")
    public boolean isDeadLetterCoherente() {
        return deadLetterRoutingKey == null || deadLetterRoutingKey.isBlank() || tieneDeadLetter();
    }

    /** Regla cruzada: una cola auto-delete no puede ser durable (se borra igual al quedar sin consumidores). */
    @AssertTrue(message = "una cola autoDelete no puede ser durable: envía durable=false")
    public boolean isAutoDeleteCoherente() {
        return !esAutoDelete() || durable != null && !durable;
    }
}

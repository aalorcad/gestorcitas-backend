package cl.duoc.gestorcitas.rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Tipos de exchange soportados por RabbitMQ. En JSON se escriben en minúscula: "direct", "topic"... */
public enum TipoExchange {
    DIRECT,
    TOPIC,
    FANOUT,
    HEADERS;

    @JsonValue
    public String valor() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static TipoExchange desde(String valor) {
        if (valor != null) {
            for (TipoExchange t : values()) {
                if (t.name().equalsIgnoreCase(valor.trim())) {
                    return t;
                }
            }
        }
        throw new IllegalArgumentException("Tipo de exchange inválido '" + valor + "'. Valores permitidos: "
                + Arrays.stream(values()).map(TipoExchange::valor).collect(Collectors.joining(", ")));
    }
}

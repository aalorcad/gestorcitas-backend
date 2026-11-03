package cl.duoc.gestorcitas.rabbitadmin.dto;

import java.util.Map;

/** Estado de una cola. Los campos nulos no están disponibles en la fuente consultada. */
public record ColaResponse(
        String nombre,
        Long mensajes,
        Integer consumidores,
        Boolean durable,
        Boolean autoDelete,
        Map<String, Object> argumentos,
        boolean protegida
) {
}

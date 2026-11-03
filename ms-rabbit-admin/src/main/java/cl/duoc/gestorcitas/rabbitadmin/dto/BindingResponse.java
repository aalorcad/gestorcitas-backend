package cl.duoc.gestorcitas.rabbitadmin.dto;

public record BindingResponse(String exchange, String cola, String routingKey) {
}

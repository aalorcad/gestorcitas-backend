package cl.duoc.gestorcitas.rabbitadmin.dto;

public record ExchangeResponse(String nombre, String tipo, Boolean durable, Boolean autoDelete, boolean protegido) {
}

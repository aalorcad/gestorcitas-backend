package cl.duoc.gestorcitas.rabbitadmin.dto;

/** @param enrutable false si no existe ningún binding que coincida (el mensaje se descartaría) */
public record PublicacionResponse(String mensajeId, String exchange, String routingKey, boolean enrutable) {
}

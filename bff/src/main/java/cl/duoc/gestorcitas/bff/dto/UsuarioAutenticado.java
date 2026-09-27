package cl.duoc.gestorcitas.bff.dto;

import java.util.List;

/** Identidad extraída del JWT validado. */
public record UsuarioAutenticado(String id, String nombre, String email, List<String> roles) {
}

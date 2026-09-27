package cl.duoc.gestorcitas.usuarios.dto;

import java.util.Set;

/**
 * Identidad del usuario autenticado, propagada por el BFF después de validar el JWT
 * (cabeceras X-User-Id, X-User-Name, X-User-Email, X-User-Roles).
 */
public record UsuarioContexto(String id, String nombre, String email, Set<String> roles) {

    public static final String ROL_PACIENTE = "Paciente";
    public static final String ROL_MEDICO = "Medico";
    public static final String ROL_ADMIN = "Admin";

    public boolean tieneRol(String rol) {
        return roles != null && roles.contains(rol);
    }
}

package cl.duoc.gestorcitas.bff.service;

import cl.duoc.gestorcitas.bff.dto.PerfilResponse;
import cl.duoc.gestorcitas.bff.dto.UsuarioAutenticado;
import org.springframework.security.oauth2.jwt.Jwt;

/** Extrae la identidad del usuario desde el JWT ya validado por Spring Security. */
public interface IdentidadService {

    UsuarioAutenticado desdeToken(Jwt jwt);

    PerfilResponse perfilToken(Jwt jwt);
}

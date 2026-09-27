package cl.duoc.gestorcitas.bff.client;

import cl.duoc.gestorcitas.bff.dto.UsuarioAutenticado;
import org.springframework.http.HttpHeaders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Propaga la identidad validada por el BFF a los microservicios (cabeceras X-User-*). */
final class UsuarioHeaders {

    private UsuarioHeaders() {
    }

    static Consumer<HttpHeaders> de(UsuarioAutenticado u) {
        return h -> {
            h.set("X-User-Id", u.id());
            // URL-encoded para soportar tildes/ñ en cabeceras HTTP
            if (u.nombre() != null) h.set("X-User-Name", URLEncoder.encode(u.nombre(), StandardCharsets.UTF_8));
            if (u.email() != null) h.set("X-User-Email", URLEncoder.encode(u.email(), StandardCharsets.UTF_8));
            h.set("X-User-Roles", String.join(",", u.roles()));
        };
    }
}

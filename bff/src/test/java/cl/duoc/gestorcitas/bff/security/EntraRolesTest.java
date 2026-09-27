package cl.duoc.gestorcitas.bff.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntraRolesTest {

    private static Jwt token(List<String> roles) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("u1");
        if (roles != null) {
            b.claim("roles", roles);
        }
        return b.build();
    }

    @Test
    void usaLosRolesDelToken() {
        assertThat(EntraRoles.de(token(List.of("Admin")), "Paciente")).containsExactly("Admin");
    }

    @Test
    void sinRolesUsaElRolPorDefecto() {
        assertThat(EntraRoles.de(token(null), "Paciente")).containsExactly("Paciente");
    }

    @Test
    void sinRolesNiRolPorDefectoQuedaVacio() {
        assertThat(EntraRoles.de(token(null), "")).isEmpty();
    }
}

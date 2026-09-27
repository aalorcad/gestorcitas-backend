package cl.duoc.gestorcitas.bff.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;

/** Verifica que el token delegado contenga el scope expuesto por la API (claim "scp"). */
public class ScopeValidator implements OAuth2TokenValidator<Jwt> {

    private final String requiredScope;

    public ScopeValidator(String requiredScope) {
        this.requiredScope = requiredScope;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String scp = jwt.getClaimAsString("scp");
        if (scp != null && Arrays.asList(scp.split(" ")).contains(requiredScope)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("insufficient_scope", "Falta el scope requerido: " + requiredScope, null));
    }
}

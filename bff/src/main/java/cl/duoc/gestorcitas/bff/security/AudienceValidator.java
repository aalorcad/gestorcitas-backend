package cl.duoc.gestorcitas.bff.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** Verifica que el token fue emitido PARA esta API (claim "aud"). */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> audiencesPermitidas;

    public AudienceValidator(List<String> audiencesPermitidas) {
        this.audiencesPermitidas = audiencesPermitidas;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> aud = jwt.getAudience();
        if (aud != null && aud.stream().anyMatch(audiencesPermitidas::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Audiencia (aud) no permitida: " + aud, null));
    }
}

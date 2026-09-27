package cl.duoc.gestorcitas.bff.config;

import cl.duoc.gestorcitas.bff.security.AudienceValidator;
import cl.duoc.gestorcitas.bff.security.EntraRolesConverter;
import cl.duoc.gestorcitas.bff.security.RestAuthenticationEntryPoint;
import cl.duoc.gestorcitas.bff.security.ScopeValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SEGUNDA validación del JWT (la primera la hace AWS API Gateway).
 * Se valida: firma (JWKS de Entra ID), expiración, issuer, audience y scope,
 * y luego se autoriza cada ruta según los App Roles del usuario.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String PACIENTE = "Paciente";
    private static final String MEDICO = "Medico";
    private static final String ADMIN = "Admin";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectMapper objectMapper,
                                                   EntraIdProperties entra) throws Exception {
        RestAuthenticationEntryPoint errorHandler = new RestAuthenticationEntryPoint(objectMapper);

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight CORS y health check
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/health", "/error").permitAll()

                        // Perfil del usuario autenticado
                        .requestMatchers(HttpMethod.GET, "/api/me").authenticated()

                        // Catálogo: lectura para todos, escritura solo Admin
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/**").authenticated()
                        .requestMatchers("/api/catalogo/**").hasRole(ADMIN)

                        // Usuarios: el propio usuario
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/me/sincronizar").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/me/perfil-paciente").hasRole(PACIENTE)
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/me/perfil-medico").hasRole(MEDICO)
                        // Usuarios: listado público de médicos para reservar
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/medicos").authenticated()
                        // Usuarios: administración
                        .requestMatchers("/api/usuarios/**", "/api/usuarios").hasRole(ADMIN)
                        .requestMatchers("/api/admin/**").hasRole(ADMIN)

                        // Citas
                        .requestMatchers(HttpMethod.GET, "/api/citas/disponibilidad").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/citas").hasRole(PACIENTE)
                        .requestMatchers(HttpMethod.GET, "/api/citas/mias").hasRole(PACIENTE)
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/cancelar").hasAnyRole(PACIENTE, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/citas/agenda").hasRole(MEDICO)
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/confirmar", "/api/citas/*/atender").hasRole(MEDICO)
                        .requestMatchers(HttpMethod.GET, "/api/citas").hasRole(ADMIN)

                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new EntraRolesConverter(entra.defaultRole())))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(EntraIdProperties entra) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(entra.jwks()).build();

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(entra.issuer()),   // firma + exp/nbf + iss
                new AudienceValidator(entra.audiences()),                // aud
                new ScopeValidator(entra.requiredScope())                // scp
        );
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties cors) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(cors.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

package cl.duoc.gestorcitas.bff.controller;

import cl.duoc.gestorcitas.bff.dto.PerfilResponse;
import cl.duoc.gestorcitas.bff.service.IdentidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class PerfilController {

    private final IdentidadService identidadService;

    @GetMapping
    public PerfilResponse me(@AuthenticationPrincipal Jwt jwt) {
        return identidadService.perfilToken(jwt);
    }
}

package cl.duoc.gestorcitas.bff.controller;

import cl.duoc.gestorcitas.bff.dto.DashboardAdminResponse;
import cl.duoc.gestorcitas.bff.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioService usuarioService;

    @GetMapping("/dashboard")
    public DashboardAdminResponse dashboard(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.dashboard(jwt);
    }
}

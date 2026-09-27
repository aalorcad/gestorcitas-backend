package cl.duoc.gestorcitas.bff.controller;

import cl.duoc.gestorcitas.bff.dto.*;
import cl.duoc.gestorcitas.bff.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints de usuarios (pacientes, médicos, admins). Autorización por rol en SecurityConfig. */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ---------------- Usuario autenticado
    @PostMapping("/me/sincronizar")
    public UsuarioResponse sincronizar(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.sincronizar(jwt);
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.me(jwt);
    }

    @PutMapping("/me/perfil-paciente")
    public UsuarioResponse perfilPaciente(@Valid @RequestBody ActualizarPerfilPacienteRequest request,
                                          @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.actualizarPerfilPaciente(request, jwt);
    }

    @PutMapping("/me/perfil-medico")
    public UsuarioResponse perfilMedico(@Valid @RequestBody ActualizarPerfilMedicoRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.actualizarPerfilMedico(request, jwt);
    }

    // ---------------- Médicos (lectura para reservar)
    @GetMapping("/medicos")
    public List<MedicoResponse> medicos(@RequestParam(required = false) Long especialidadId) {
        return usuarioService.listarMedicos(especialidadId);
    }

    // ---------------- Administración
    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) String rol,
                                        @RequestParam(required = false) String q,
                                        @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.listar(rol, q, jwt);
    }

    @PatchMapping("/{id}/activar")
    public UsuarioResponse activar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.activar(id, jwt);
    }

    @PatchMapping("/{id}/desactivar")
    public UsuarioResponse desactivar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.desactivar(id, jwt);
    }

    @PostMapping("/medicos")
    public ResponseEntity<UsuarioResponse> registrarMedico(@Valid @RequestBody RegistrarMedicoRequest request,
                                                           @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrarMedico(request, jwt));
    }

    @PutMapping("/{id}/perfil-medico")
    public UsuarioResponse asignarPerfilMedico(@PathVariable Long id,
                                               @Valid @RequestBody AsignarPerfilMedicoRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.asignarPerfilMedico(id, request, jwt);
    }
}

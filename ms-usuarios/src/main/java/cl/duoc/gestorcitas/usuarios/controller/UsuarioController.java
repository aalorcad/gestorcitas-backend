package cl.duoc.gestorcitas.usuarios.controller;

import cl.duoc.gestorcitas.usuarios.config.UsuarioActual;
import cl.duoc.gestorcitas.usuarios.dto.*;
import cl.duoc.gestorcitas.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Capa HTTP de usuarios: sin reglas de negocio, solo delega en {@link UsuarioService}. */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/sincronizar")
    public UsuarioResponse sincronizar(@UsuarioActual UsuarioContexto usuario) {
        return usuarioService.sincronizar(usuario);
    }

    @GetMapping("/me")
    public UsuarioResponse me(@UsuarioActual UsuarioContexto usuario) {
        return usuarioService.obtenerActual(usuario);
    }

    @PutMapping("/me/perfil-paciente")
    public UsuarioResponse perfilPaciente(@Valid @RequestBody ActualizarPerfilPacienteRequest request,
                                          @UsuarioActual UsuarioContexto usuario) {
        return usuarioService.actualizarPerfilPaciente(request, usuario);
    }

    @PutMapping("/me/perfil-medico")
    public UsuarioResponse perfilMedico(@Valid @RequestBody ActualizarPerfilMedicoRequest request,
                                        @UsuarioActual UsuarioContexto usuario) {
        return usuarioService.actualizarPerfilMedico(request, usuario);
    }

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) String rol,
                                        @RequestParam(required = false) String q,
                                        @UsuarioActual UsuarioContexto usuario) {
        return usuarioService.listar(rol, q, usuario);
    }

    @GetMapping("/resumen")
    public ResumenUsuariosResponse resumen(@UsuarioActual UsuarioContexto usuario) {
        return usuarioService.resumen(usuario);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id, @UsuarioActual UsuarioContexto usuario) {
        return usuarioService.obtener(id, usuario);
    }

    @PatchMapping("/{id}/activar")
    public UsuarioResponse activar(@PathVariable Long id, @UsuarioActual UsuarioContexto usuario) {
        return usuarioService.activar(id, usuario);
    }

    @PatchMapping("/{id}/desactivar")
    public UsuarioResponse desactivar(@PathVariable Long id, @UsuarioActual UsuarioContexto usuario) {
        return usuarioService.desactivar(id, usuario);
    }

    /** Uso interno (ms-citas): estado del paciente para validar reservas. */
    @GetMapping("/pacientes/{oid}/estado")
    public PacienteEstadoResponse estadoPaciente(@PathVariable String oid) {
        return usuarioService.estadoPaciente(oid);
    }
}

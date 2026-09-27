package cl.duoc.gestorcitas.usuarios.controller;

import cl.duoc.gestorcitas.usuarios.config.UsuarioActual;
import cl.duoc.gestorcitas.usuarios.dto.*;
import cl.duoc.gestorcitas.usuarios.service.MedicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final MedicoService medicoService;

    @GetMapping
    public List<MedicoResponse> listar(@RequestParam(required = false) Long especialidadId) {
        return medicoService.listarAgendables(especialidadId);
    }

    @GetMapping("/{id}")
    public MedicoResponse obtener(@PathVariable Long id) {
        return medicoService.obtener(id);
    }

    @GetMapping("/conteo")
    public long contarPorEspecialidad(@RequestParam Long especialidadId) {
        return medicoService.contarActivosPorEspecialidad(especialidadId);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistrarMedicoRequest request,
                                                     @UsuarioActual UsuarioContexto usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicoService.registrar(request, usuario));
    }

    @PutMapping("/{id}/perfil")
    public UsuarioResponse asignarPerfil(@PathVariable Long id,
                                         @Valid @RequestBody AsignarPerfilMedicoRequest request,
                                         @UsuarioActual UsuarioContexto usuario) {
        return medicoService.asignarPerfil(id, request, usuario);
    }
}

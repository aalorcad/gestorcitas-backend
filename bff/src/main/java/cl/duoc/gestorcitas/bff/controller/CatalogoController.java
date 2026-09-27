package cl.duoc.gestorcitas.bff.controller;

import cl.duoc.gestorcitas.bff.dto.*;
import cl.duoc.gestorcitas.bff.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints públicos (vía API Gateway) del catálogo. La autorización por rol está en SecurityConfig. */
@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    // ---------------- Especialidades
    @GetMapping("/especialidades")
    public List<EspecialidadResponse> especialidades(@RequestParam(defaultValue = "true") boolean soloActivas) {
        return catalogoService.listarEspecialidades(soloActivas);
    }

    @PostMapping("/especialidades")
    public ResponseEntity<EspecialidadResponse> crearEspecialidad(@Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearEspecialidad(request));
    }

    @PutMapping("/especialidades/{id}")
    public EspecialidadResponse actualizarEspecialidad(@PathVariable Long id, @Valid @RequestBody EspecialidadRequest request) {
        return catalogoService.actualizarEspecialidad(id, request);
    }

    @PatchMapping("/especialidades/{id}/activar")
    public EspecialidadResponse activarEspecialidad(@PathVariable Long id) {
        return catalogoService.activarEspecialidad(id);
    }

    @DeleteMapping("/especialidades/{id}")
    public ResponseEntity<Void> desactivarEspecialidad(@PathVariable Long id) {
        catalogoService.desactivarEspecialidad(id);
        return ResponseEntity.noContent().build();
    }
}

package cl.duoc.gestorcitas.catalogo.controller;

import cl.duoc.gestorcitas.catalogo.dto.EspecialidadRequest;
import cl.duoc.gestorcitas.catalogo.dto.EspecialidadResponse;
import cl.duoc.gestorcitas.catalogo.service.EspecialidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Capa HTTP: solo recibe, valida el formato y delega en el servicio.
 * No contiene reglas de negocio.
 */
@RestController
@RequestMapping("/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public List<EspecialidadResponse> listar(@RequestParam(defaultValue = "true") boolean soloActivas) {
        return especialidadService.listar(soloActivas);
    }

    @GetMapping("/{id}")
    public EspecialidadResponse obtener(@PathVariable Long id) {
        return especialidadService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EspecialidadResponse> crear(@Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crear(request));
    }

    @PutMapping("/{id}")
    public EspecialidadResponse actualizar(@PathVariable Long id, @Valid @RequestBody EspecialidadRequest request) {
        return especialidadService.actualizar(id, request);
    }

    @PatchMapping("/{id}/activar")
    public EspecialidadResponse activar(@PathVariable Long id) {
        return especialidadService.activar(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        especialidadService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}

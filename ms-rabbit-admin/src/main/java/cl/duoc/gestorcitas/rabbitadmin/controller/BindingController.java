package cl.duoc.gestorcitas.rabbitadmin.controller;

import cl.duoc.gestorcitas.rabbitadmin.dto.BindingRequest;
import cl.duoc.gestorcitas.rabbitadmin.dto.BindingResponse;
import cl.duoc.gestorcitas.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bindings (enlaces exchange -> cola).
 * <pre>
 * GET    /bindings                                        lista los bindings hacia colas
 * POST   /bindings        {exchange, cola, routingKey}    crea un binding (201)
 * DELETE /bindings?exchange=..&cola=..&routingKey=..      elimina un binding (204)
 * </pre>
 * Un binding no tiene un identificador simple, por eso se elimina indicando sus tres partes.
 */
@RestController
@RequestMapping("/bindings")
public class BindingController {

    private final RabbitAdminService service;

    public BindingController(RabbitAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<BindingResponse> listar() {
        return service.listarBindings();
    }

    @PostMapping
    public ResponseEntity<BindingResponse> crear(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearBinding(request));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(@Valid @ModelAttribute BindingRequest request) {
        service.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }
}

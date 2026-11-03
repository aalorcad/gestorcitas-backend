package cl.duoc.gestorcitas.rabbitadmin.controller;

import cl.duoc.gestorcitas.rabbitadmin.dto.ColaResponse;
import cl.duoc.gestorcitas.rabbitadmin.dto.CrearColaRequest;
import cl.duoc.gestorcitas.rabbitadmin.dto.NombreAmqp;
import cl.duoc.gestorcitas.rabbitadmin.dto.PurgaResponse;
import cl.duoc.gestorcitas.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Colas.
 * <pre>
 * GET    /queues                     lista las colas con mensajes y consumidores
 * GET    /queues/{nombre}            detalle de una cola
 * POST   /queues                     crea una cola (201)
 * DELETE /queues/{nombre}            elimina una cola (204). Opcional: ?soloSiVacia=true&soloSiSinConsumidores=true
 * DELETE /queues/{nombre}/messages   vacía (purga) la cola
 * </pre>
 */
@RestController
@RequestMapping("/queues")
public class ColaController {

    private final RabbitAdminService service;

    public ColaController(RabbitAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<ColaResponse> listar() {
        return service.listarColas();
    }

    @GetMapping("/{nombre}")
    public ColaResponse obtener(@PathVariable @NombreAmqp String nombre) {
        return service.obtenerCola(nombre);
    }

    @PostMapping
    public ResponseEntity<ColaResponse> crear(@Valid @RequestBody CrearColaRequest request) {
        ColaResponse creada = service.crearCola(request);
        return ResponseEntity.created(URI.create("/queues/" + creada.nombre())).body(creada);
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable @NombreAmqp String nombre,
                                         @RequestParam(defaultValue = "false") boolean soloSiVacia,
                                         @RequestParam(defaultValue = "false") boolean soloSiSinConsumidores) {
        service.eliminarCola(nombre, soloSiVacia, soloSiSinConsumidores);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{nombre}/messages")
    public PurgaResponse purgar(@PathVariable @NombreAmqp String nombre) {
        return service.purgarCola(nombre);
    }
}

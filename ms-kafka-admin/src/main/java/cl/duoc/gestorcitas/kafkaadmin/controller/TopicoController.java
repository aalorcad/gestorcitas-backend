package cl.duoc.gestorcitas.kafkaadmin.controller;

import cl.duoc.gestorcitas.kafkaadmin.dto.ActualizarConfigRequest;
import cl.duoc.gestorcitas.kafkaadmin.dto.AumentarParticionesRequest;
import cl.duoc.gestorcitas.kafkaadmin.dto.CrearTopicoRequest;
import cl.duoc.gestorcitas.kafkaadmin.dto.NombreTopico;
import cl.duoc.gestorcitas.kafkaadmin.dto.TopicoDetalleResponse;
import cl.duoc.gestorcitas.kafkaadmin.dto.TopicoResumenResponse;
import cl.duoc.gestorcitas.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Tópicos.
 * <pre>
 * GET    /topics                       lista los tópicos (particiones, replicación, protegido)
 * GET    /topics/{nombre}              detalle: particiones (líder, réplicas, ISR, offsets) y configuración propia
 * POST   /topics                       crea un tópico (201)
 * PATCH  /topics/{nombre}/config       modifica configuraciones (retention.ms, cleanup.policy, ...)
 * POST   /topics/{nombre}/partitions   aumenta las particiones
 * DELETE /topics/{nombre}              elimina el tópico (204); los tópicos de GestorCitas están protegidos (409)
 * </pre>
 */
@RestController
@RequestMapping("/topics")
public class TopicoController {

    private final KafkaAdminService service;

    public TopicoController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<TopicoResumenResponse> listar() {
        return service.listarTopicos();
    }

    @GetMapping("/{nombre}")
    public TopicoDetalleResponse obtener(@PathVariable @NombreTopico String nombre) {
        return service.obtenerTopico(nombre);
    }

    @PostMapping
    public ResponseEntity<TopicoDetalleResponse> crear(@Valid @RequestBody CrearTopicoRequest request) {
        TopicoDetalleResponse creado = service.crearTopico(request);
        return ResponseEntity.created(URI.create("/topics/" + creado.nombre())).body(creado);
    }

    @PatchMapping("/{nombre}/config")
    public TopicoDetalleResponse actualizarConfiguracion(@PathVariable @NombreTopico String nombre,
                                                         @Valid @RequestBody ActualizarConfigRequest request) {
        return service.actualizarConfiguracion(nombre, request.configuracion());
    }

    @PostMapping("/{nombre}/partitions")
    public TopicoDetalleResponse aumentarParticiones(@PathVariable @NombreTopico String nombre,
                                                     @Valid @RequestBody AumentarParticionesRequest request) {
        return service.aumentarParticiones(nombre, request.total());
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable @NombreTopico String nombre) {
        service.eliminarTopico(nombre);
        return ResponseEntity.noContent().build();
    }
}

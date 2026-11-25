package cl.duoc.gestorcitas.bff.controller;

import cl.duoc.gestorcitas.bff.service.KafkaAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Administración de Kafka para el rol Admin (la regla /api/admin/** de SecurityConfig exige ROLE_Admin
 * y un JWT válido de Entra ID). Reenvía a ms-kafka-admin, que valida y aplica las reglas.
 */
@RestController
@RequestMapping("/api/admin/kafka")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/cluster")
    public JsonNode cluster() {
        return service.cluster();
    }

    // ------------------------------------------------------------------ Tópicos

    @GetMapping("/topics")
    public JsonNode listarTopicos() {
        return service.listarTopicos();
    }

    @GetMapping("/topics/{nombre}")
    public JsonNode obtenerTopico(@PathVariable String nombre) {
        return service.obtenerTopico(nombre);
    }

    @PostMapping("/topics")
    public ResponseEntity<JsonNode> crearTopico(@RequestBody JsonNode request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearTopico(request));
    }

    @PatchMapping("/topics/{nombre}/config")
    public JsonNode actualizarConfiguracion(@PathVariable String nombre, @RequestBody JsonNode request) {
        return service.actualizarConfiguracion(nombre, request);
    }

    @PostMapping("/topics/{nombre}/partitions")
    public JsonNode aumentarParticiones(@PathVariable String nombre, @RequestBody JsonNode request) {
        return service.aumentarParticiones(nombre, request);
    }

    @DeleteMapping("/topics/{nombre}")
    public ResponseEntity<Void> eliminarTopico(@PathVariable String nombre) {
        service.eliminarTopico(nombre);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ Grupos de consumidores

    @GetMapping("/consumer-groups")
    public JsonNode listarGrupos() {
        return service.listarGrupos();
    }

    @GetMapping("/consumer-groups/{grupo}")
    public JsonNode obtenerGrupo(@PathVariable String grupo) {
        return service.obtenerGrupo(grupo);
    }

    @PostMapping("/consumer-groups/{grupo}/reset-offsets")
    public JsonNode reiniciarOffsets(@PathVariable String grupo, @RequestBody JsonNode request) {
        return service.reiniciarOffsets(grupo, request);
    }

    @DeleteMapping("/consumer-groups/{grupo}")
    public ResponseEntity<Void> eliminarGrupo(@PathVariable String grupo) {
        service.eliminarGrupo(grupo);
        return ResponseEntity.noContent().build();
    }
}

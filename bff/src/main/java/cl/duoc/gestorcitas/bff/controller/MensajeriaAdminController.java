package cl.duoc.gestorcitas.bff.controller;

import cl.duoc.gestorcitas.bff.service.MensajeriaAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Administración de RabbitMQ para el rol Admin (la regla /api/admin/** de SecurityConfig exige ROLE_Admin
 * y un JWT válido de Entra ID). Reenvía a ms-rabbit-admin, que valida y aplica las reglas.
 */
@RestController
@RequestMapping("/api/admin/mensajeria")
public class MensajeriaAdminController {

    private final MensajeriaAdminService service;

    public MensajeriaAdminController(MensajeriaAdminService service) {
        this.service = service;
    }

    // ------------------------------------------------------------------ Colas

    @GetMapping("/queues")
    public JsonNode listarColas() {
        return service.listarColas();
    }

    @GetMapping("/queues/{nombre}")
    public JsonNode obtenerCola(@PathVariable String nombre) {
        return service.obtenerCola(nombre);
    }

    @PostMapping("/queues")
    public ResponseEntity<JsonNode> crearCola(@RequestBody JsonNode request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCola(request));
    }

    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String nombre,
                                             @RequestParam(defaultValue = "false") boolean soloSiVacia,
                                             @RequestParam(defaultValue = "false") boolean soloSiSinConsumidores) {
        service.eliminarCola(nombre, soloSiVacia, soloSiSinConsumidores);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/queues/{nombre}/messages")
    public JsonNode purgarCola(@PathVariable String nombre) {
        return service.purgarCola(nombre);
    }

    // ------------------------------------------------------------------ Exchanges

    @GetMapping("/exchanges")
    public JsonNode listarExchanges() {
        return service.listarExchanges();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<JsonNode> crearExchange(@RequestBody JsonNode request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearExchange(request));
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String nombre) {
        service.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ Bindings

    @GetMapping("/bindings")
    public JsonNode listarBindings() {
        return service.listarBindings();
    }

    @PostMapping("/bindings")
    public ResponseEntity<JsonNode> crearBinding(@RequestBody JsonNode request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearBinding(request));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@RequestParam String exchange,
                                                @RequestParam String cola,
                                                @RequestParam(defaultValue = "") String routingKey) {
        service.eliminarBinding(exchange, cola, routingKey);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ Mensajes de prueba

    @PostMapping("/messages")
    public ResponseEntity<JsonNode> publicarMensaje(@RequestBody JsonNode request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.publicarMensaje(request));
    }
}

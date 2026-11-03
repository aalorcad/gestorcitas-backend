package cl.duoc.gestorcitas.rabbitadmin.controller;

import cl.duoc.gestorcitas.rabbitadmin.dto.CrearExchangeRequest;
import cl.duoc.gestorcitas.rabbitadmin.dto.ExchangeResponse;
import cl.duoc.gestorcitas.rabbitadmin.dto.NombreAmqp;
import cl.duoc.gestorcitas.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Exchanges.
 * <pre>
 * GET    /exchanges            lista los exchanges (sin los del sistema: "" y amq.*)
 * POST   /exchanges            crea un exchange direct | topic | fanout | headers (201)
 * DELETE /exchanges/{nombre}   elimina un exchange (204)
 * </pre>
 */
@RestController
@RequestMapping("/exchanges")
public class ExchangeController {

    private final RabbitAdminService service;

    public ExchangeController(RabbitAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExchangeResponse> listar() {
        return service.listarExchanges();
    }

    @PostMapping
    public ResponseEntity<ExchangeResponse> crear(@Valid @RequestBody CrearExchangeRequest request) {
        ExchangeResponse creado = service.crearExchange(request);
        return ResponseEntity.created(URI.create("/exchanges/" + creado.nombre())).body(creado);
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable @NombreAmqp String nombre) {
        service.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }
}

package cl.duoc.gestorcitas.rabbitadmin.controller;

import cl.duoc.gestorcitas.rabbitadmin.dto.PublicacionResponse;
import cl.duoc.gestorcitas.rabbitadmin.dto.PublicarMensajeRequest;
import cl.duoc.gestorcitas.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publicación de mensajes de prueba.
 * <pre>
 * POST /messages  {exchange, routingKey, payload}   publica un mensaje (202 Accepted)
 * </pre>
 * Útil para demostrar el enrutamiento, los reintentos y el envío a la DLQ (por ejemplo, con un payload incompleto).
 */
@RestController
@RequestMapping("/messages")
public class MensajeController {

    private final RabbitAdminService service;

    public MensajeController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PublicacionResponse> publicar(@Valid @RequestBody PublicarMensajeRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.publicar(request));
    }
}

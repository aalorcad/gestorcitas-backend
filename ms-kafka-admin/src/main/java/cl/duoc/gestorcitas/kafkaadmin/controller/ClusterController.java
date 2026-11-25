package cl.duoc.gestorcitas.kafkaadmin.controller;

import cl.duoc.gestorcitas.kafkaadmin.dto.ClusterResponse;
import cl.duoc.gestorcitas.kafkaadmin.service.KafkaAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clúster.
 * <pre>
 * GET /cluster   id del clúster, broker controlador y brokers disponibles
 * </pre>
 */
@RestController
@RequestMapping("/cluster")
public class ClusterController {

    private final KafkaAdminService service;

    public ClusterController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping
    public ClusterResponse cluster() {
        return service.cluster();
    }
}

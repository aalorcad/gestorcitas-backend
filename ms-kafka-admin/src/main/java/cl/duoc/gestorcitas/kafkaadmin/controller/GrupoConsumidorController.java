package cl.duoc.gestorcitas.kafkaadmin.controller;

import cl.duoc.gestorcitas.kafkaadmin.dto.GrupoDetalleResponse;
import cl.duoc.gestorcitas.kafkaadmin.dto.GrupoResumenResponse;
import cl.duoc.gestorcitas.kafkaadmin.dto.NombreTopico;
import cl.duoc.gestorcitas.kafkaadmin.dto.ReiniciarOffsetsRequest;
import cl.duoc.gestorcitas.kafkaadmin.dto.ReinicioOffsetsResponse;
import cl.duoc.gestorcitas.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Grupos de consumidores.
 * <pre>
 * GET    /consumer-groups                        lista los grupos y su estado
 * GET    /consumer-groups/{grupo}                miembros, particiones asignadas y lag por partición
 * POST   /consumer-groups/{grupo}/reset-offsets  mueve los offsets (EARLIEST, LATEST u OFFSET); el grupo debe estar vacío
 * DELETE /consumer-groups/{grupo}                elimina el grupo (204); Kafka lo rechaza si tiene consumidores activos (409)
 * </pre>
 */
@RestController
@RequestMapping("/consumer-groups")
public class GrupoConsumidorController {

    private final KafkaAdminService service;

    public GrupoConsumidorController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<GrupoResumenResponse> listar() {
        return service.listarGrupos();
    }

    @GetMapping("/{grupo}")
    public GrupoDetalleResponse obtener(@PathVariable @NombreTopico String grupo) {
        return service.obtenerGrupo(grupo);
    }

    @PostMapping("/{grupo}/reset-offsets")
    public ReinicioOffsetsResponse reiniciarOffsets(@PathVariable @NombreTopico String grupo,
                                                    @Valid @RequestBody ReiniciarOffsetsRequest request) {
        return service.reiniciarOffsets(grupo, request);
    }

    @DeleteMapping("/{grupo}")
    public ResponseEntity<Void> eliminar(@PathVariable @NombreTopico String grupo) {
        service.eliminarGrupo(grupo);
        return ResponseEntity.noContent().build();
    }
}

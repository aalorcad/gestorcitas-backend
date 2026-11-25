package cl.duoc.gestorcitas.analitica.controller;

import cl.duoc.gestorcitas.analitica.dto.EventoCitaResponse;
import cl.duoc.gestorcitas.analitica.dto.ResumenAnaliticaResponse;
import cl.duoc.gestorcitas.analitica.service.AnaliticaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Indicadores calculados a partir del streaming de Kafka.
 * No se publica directamente: el BFF lo expone en {@code /api/analitica/**} solo para el rol Admin.
 */
@RestController
@RequestMapping("/analitica")
public class AnaliticaController {

    private final AnaliticaService servicio;

    public AnaliticaController(AnaliticaService servicio) {
        this.servicio = servicio;
    }

    /** GET /analitica/resumen: totales por tipo de evento, por especialidad, por médico y estado de usuarios. */
    @GetMapping("/resumen")
    public ResumenAnaliticaResponse resumen() {
        return servicio.resumen();
    }

    /** GET /analitica/eventos?limite=20: últimos eventos procesados con su partición y offset (máx. 100). */
    @GetMapping("/eventos")
    public List<EventoCitaResponse> ultimosEventos(@RequestParam(defaultValue = "20") int limite) {
        return servicio.ultimosEventos(limite);
    }
}

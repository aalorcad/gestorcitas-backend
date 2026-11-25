package cl.duoc.gestorcitas.bff.controller;

import cl.duoc.gestorcitas.bff.service.AnaliticaService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores del streaming de Kafka (rol Admin). Reenvía a ms-analitica. */
@RestController
@RequestMapping("/api/analitica")
public class AnaliticaController {

    private final AnaliticaService service;

    public AnaliticaController(AnaliticaService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public JsonNode resumen() {
        return service.resumen();
    }

    @GetMapping("/eventos")
    public JsonNode ultimosEventos(@RequestParam(defaultValue = "20") int limite) {
        return service.ultimosEventos(limite);
    }
}

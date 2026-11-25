package cl.duoc.gestorcitas.bff.service.impl;

import cl.duoc.gestorcitas.bff.client.AnaliticaClient;
import cl.duoc.gestorcitas.bff.service.AnaliticaService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public class AnaliticaServiceImpl implements AnaliticaService {

    private final AnaliticaClient client;

    public AnaliticaServiceImpl(AnaliticaClient client) {
        this.client = client;
    }

    @Override
    public JsonNode resumen() {
        return client.resumen();
    }

    @Override
    public JsonNode ultimosEventos(int limite) {
        return client.ultimosEventos(limite);
    }
}

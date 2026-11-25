package cl.duoc.gestorcitas.bff.service;

import com.fasterxml.jackson.databind.JsonNode;

/** Indicadores del streaming de Kafka (delegados en ms-analitica). */
public interface AnaliticaService {

    JsonNode resumen();

    JsonNode ultimosEventos(int limite);
}

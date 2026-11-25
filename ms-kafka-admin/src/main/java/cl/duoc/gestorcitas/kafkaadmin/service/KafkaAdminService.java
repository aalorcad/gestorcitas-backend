package cl.duoc.gestorcitas.kafkaadmin.service;

import cl.duoc.gestorcitas.kafkaadmin.dto.*;

import java.util.List;
import java.util.Map;

/** Operaciones de administración del clúster Kafka (implementadas con AdminClient). */
public interface KafkaAdminService {

    ClusterResponse cluster();

    // ------------------------------------------------------------------ Tópicos

    List<TopicoResumenResponse> listarTopicos();

    TopicoDetalleResponse obtenerTopico(String nombre);

    TopicoDetalleResponse crearTopico(CrearTopicoRequest request);

    TopicoDetalleResponse actualizarConfiguracion(String nombre, Map<String, String> configuracion);

    TopicoDetalleResponse aumentarParticiones(String nombre, int total);

    void eliminarTopico(String nombre);

    // ------------------------------------------------------------------ Grupos de consumidores

    List<GrupoResumenResponse> listarGrupos();

    GrupoDetalleResponse obtenerGrupo(String grupo);

    ReinicioOffsetsResponse reiniciarOffsets(String grupo, ReiniciarOffsetsRequest request);

    void eliminarGrupo(String grupo);
}

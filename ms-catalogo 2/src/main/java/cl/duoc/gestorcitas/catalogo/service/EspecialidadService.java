package cl.duoc.gestorcitas.catalogo.service;

import cl.duoc.gestorcitas.catalogo.dto.EspecialidadRequest;
import cl.duoc.gestorcitas.catalogo.dto.EspecialidadResponse;

import java.util.List;

public interface EspecialidadService {

    List<EspecialidadResponse> listar(boolean soloActivas);

    EspecialidadResponse obtener(Long id);

    EspecialidadResponse crear(EspecialidadRequest request);

    EspecialidadResponse actualizar(Long id, EspecialidadRequest request);

    void desactivar(Long id);

    EspecialidadResponse activar(Long id);
}

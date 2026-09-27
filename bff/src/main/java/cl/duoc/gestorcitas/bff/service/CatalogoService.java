package cl.duoc.gestorcitas.bff.service;

import cl.duoc.gestorcitas.bff.dto.EspecialidadRequest;
import cl.duoc.gestorcitas.bff.dto.EspecialidadResponse;

import java.util.List;

public interface CatalogoService {

    List<EspecialidadResponse> listarEspecialidades(boolean soloActivas);

    EspecialidadResponse crearEspecialidad(EspecialidadRequest request);

    EspecialidadResponse actualizarEspecialidad(Long id, EspecialidadRequest request);

    EspecialidadResponse activarEspecialidad(Long id);

    void desactivarEspecialidad(Long id);
}

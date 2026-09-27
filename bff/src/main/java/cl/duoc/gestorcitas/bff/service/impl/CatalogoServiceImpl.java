package cl.duoc.gestorcitas.bff.service.impl;

import cl.duoc.gestorcitas.bff.client.CatalogoClient;
import cl.duoc.gestorcitas.bff.client.UsuariosClient;
import cl.duoc.gestorcitas.bff.dto.EspecialidadRequest;
import cl.duoc.gestorcitas.bff.dto.EspecialidadResponse;
import cl.duoc.gestorcitas.bff.exception.BusinessRuleException;
import cl.duoc.gestorcitas.bff.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Orquesta el catálogo y aplica reglas que involucran a más de un microservicio. */
@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final CatalogoClient catalogoClient;
    private final UsuariosClient usuariosClient;

    @Override
    public List<EspecialidadResponse> listarEspecialidades(boolean soloActivas) {
        return catalogoClient.listarEspecialidades(soloActivas);
    }

    @Override
    public EspecialidadResponse crearEspecialidad(EspecialidadRequest request) {
        return catalogoClient.crearEspecialidad(request);
    }

    @Override
    public EspecialidadResponse actualizarEspecialidad(Long id, EspecialidadRequest request) {
        return catalogoClient.actualizarEspecialidad(id, request);
    }

    @Override
    public EspecialidadResponse activarEspecialidad(Long id) {
        return catalogoClient.activarEspecialidad(id);
    }

    /** Regla: no se desactiva una especialidad que tiene médicos activos (ms-usuarios). */
    @Override
    public void desactivarEspecialidad(Long id) {
        long medicosActivos = usuariosClient.contarMedicosActivos(id);
        if (medicosActivos > 0) {
            throw new BusinessRuleException("No se puede desactivar la especialidad: tiene "
                    + medicosActivos + " médico(s) activo(s). Reasígnalos o desactívalos primero.");
        }
        catalogoClient.desactivarEspecialidad(id);
    }
}

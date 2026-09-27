package cl.duoc.gestorcitas.catalogo.service.impl;

import cl.duoc.gestorcitas.catalogo.dto.EspecialidadRequest;
import cl.duoc.gestorcitas.catalogo.dto.EspecialidadResponse;
import cl.duoc.gestorcitas.catalogo.entity.Especialidad;
import cl.duoc.gestorcitas.catalogo.exception.BusinessException;
import cl.duoc.gestorcitas.catalogo.exception.ResourceNotFoundException;
import cl.duoc.gestorcitas.catalogo.mapper.CatalogoMapper;
import cl.duoc.gestorcitas.catalogo.repository.EspecialidadRepository;
import cl.duoc.gestorcitas.catalogo.service.EspecialidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EspecialidadServiceImpl implements EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final CatalogoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listar(boolean soloActivas) {
        List<Especialidad> lista = soloActivas
                ? especialidadRepository.findByActivaTrueOrderByNombreAsc()
                : especialidadRepository.findAllByOrderByNombreAsc();
        return lista.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EspecialidadResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public EspecialidadResponse crear(EspecialidadRequest request) {
        String nombre = normalizar(request.nombre());
        if (especialidadRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe una especialidad llamada '" + nombre + "'");
        }
        Especialidad nueva = Especialidad.builder()
                .nombre(nombre)
                .descripcion(request.descripcion())
                .valorConsulta(request.valorConsulta())
                .activa(true)
                .build();
        return mapper.toResponse(especialidadRepository.save(nueva));
    }

    @Override
    @Transactional
    public EspecialidadResponse actualizar(Long id, EspecialidadRequest request) {
        Especialidad actual = buscar(id);
        String nombre = normalizar(request.nombre());
        if (especialidadRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe otra especialidad llamada '" + nombre + "'");
        }
        actual.setNombre(nombre);
        actual.setDescripcion(request.descripcion());
        actual.setValorConsulta(request.valorConsulta());
        return mapper.toResponse(especialidadRepository.save(actual));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        // La regla "no desactivar con médicos activos" cruza microservicios (los médicos viven
        // en ms-usuarios) y se valida en la capa de servicio del BFF antes de llamar aquí.
        Especialidad actual = buscar(id);
        if (!actual.isActiva()) {
            throw new BusinessException("La especialidad ya está inactiva");
        }
        actual.setActiva(false);
        especialidadRepository.save(actual);
    }

    @Override
    @Transactional
    public EspecialidadResponse activar(Long id) {
        Especialidad actual = buscar(id);
        actual.setActiva(true);
        return mapper.toResponse(especialidadRepository.save(actual));
    }

    private Especialidad buscar(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad " + id + " no encontrada"));
    }

    private String normalizar(String nombre) {
        return nombre == null ? null : nombre.trim();
    }
}

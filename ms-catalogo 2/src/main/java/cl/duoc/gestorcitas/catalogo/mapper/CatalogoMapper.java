package cl.duoc.gestorcitas.catalogo.mapper;

import cl.duoc.gestorcitas.catalogo.dto.EspecialidadResponse;
import cl.duoc.gestorcitas.catalogo.entity.Especialidad;
import org.springframework.stereotype.Component;

@Component
public class CatalogoMapper {

    public EspecialidadResponse toResponse(Especialidad e) {
        return new EspecialidadResponse(e.getId(), e.getNombre(), e.getDescripcion(), e.getValorConsulta(), e.isActiva());
    }
}

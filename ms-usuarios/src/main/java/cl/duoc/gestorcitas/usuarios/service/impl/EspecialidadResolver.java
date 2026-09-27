package cl.duoc.gestorcitas.usuarios.service.impl;

import cl.duoc.gestorcitas.usuarios.client.CatalogoClient;
import cl.duoc.gestorcitas.usuarios.dto.EspecialidadDto;
import cl.duoc.gestorcitas.usuarios.exception.BusinessException;
import cl.duoc.gestorcitas.usuarios.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/** Apoyo de la capa de servicio para resolver/validar especialidades contra ms-catalogo. */
@Component
@RequiredArgsConstructor
class EspecialidadResolver {

    private static final Logger log = LoggerFactory.getLogger(EspecialidadResolver.class);

    private final CatalogoClient catalogoClient;

    /** Nombres de todas las especialidades; si el catálogo no responde devuelve mapa vacío. */
    Map<Long, String> nombres() {
        try {
            return catalogoClient.listarEspecialidades().stream()
                    .collect(Collectors.toMap(EspecialidadDto::id, EspecialidadDto::nombre, (a, b) -> a));
        } catch (ExternalServiceException ex) {
            log.warn("No fue posible resolver nombres de especialidad: {}", ex.getMessage());
            return Map.of();
        }
    }

    /** Regla: solo se asignan especialidades existentes y activas. */
    EspecialidadDto exigirActiva(Long especialidadId) {
        EspecialidadDto esp = catalogoClient.obtenerEspecialidad(especialidadId);
        if (!esp.activa()) {
            throw new BusinessException("La especialidad '" + esp.nombre() + "' está inactiva");
        }
        return esp;
    }
}

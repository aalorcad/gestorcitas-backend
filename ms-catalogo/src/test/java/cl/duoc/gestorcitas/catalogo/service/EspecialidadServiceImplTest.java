package cl.duoc.gestorcitas.catalogo.service;

import cl.duoc.gestorcitas.catalogo.dto.EspecialidadRequest;
import cl.duoc.gestorcitas.catalogo.entity.Especialidad;
import cl.duoc.gestorcitas.catalogo.exception.BusinessException;
import cl.duoc.gestorcitas.catalogo.mapper.CatalogoMapper;
import cl.duoc.gestorcitas.catalogo.repository.EspecialidadRepository;
import cl.duoc.gestorcitas.catalogo.service.impl.EspecialidadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EspecialidadServiceImplTest {

    @Mock
    private EspecialidadRepository especialidadRepository;

    private EspecialidadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EspecialidadServiceImpl(especialidadRepository, new CatalogoMapper());
    }

    @Test
    void crearFallaSiNombreDuplicado() {
        when(especialidadRepository.existsByNombreIgnoreCase("Pediatría")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new EspecialidadRequest(" Pediatría ", null, 30000)))
                .isInstanceOf(BusinessException.class);
        verify(especialidadRepository, never()).save(any());
    }

    @Test
    void crearGuardaValorConsulta() {
        when(especialidadRepository.save(any(Especialidad.class))).thenAnswer(inv -> inv.getArgument(0));

        var r = service.crear(new EspecialidadRequest("Traumatología", "Huesos", 40000));

        assertThat(r.valorConsulta()).isEqualTo(40000);
        assertThat(r.activa()).isTrue();
    }

    @Test
    void noSeDesactivaDosVeces() {
        when(especialidadRepository.findById(1L))
                .thenReturn(Optional.of(Especialidad.builder().id(1L).nombre("X").activa(false).build()));

        assertThatThrownBy(() -> service.desactivar(1L)).isInstanceOf(BusinessException.class);
    }
}

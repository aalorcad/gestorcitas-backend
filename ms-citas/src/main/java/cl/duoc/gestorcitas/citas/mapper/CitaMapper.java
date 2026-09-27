package cl.duoc.gestorcitas.citas.mapper;

import cl.duoc.gestorcitas.citas.dto.CitaResponse;
import cl.duoc.gestorcitas.citas.entity.Cita;
import org.springframework.stereotype.Component;

@Component
public class CitaMapper {

    public CitaResponse toResponse(Cita c) {
        return new CitaResponse(
                c.getId(),
                c.getPacienteId(),
                c.getPacienteNombre(),
                c.getPacienteEmail(),
                c.getMedicoId(),
                c.getMedicoNombre(),
                c.getEspecialidadId(),
                c.getEspecialidadNombre(),
                c.getFechaHora(),
                c.getMotivo(),
                c.getEstado(),
                c.getObservaciones(),
                c.getCreadaEn()
        );
    }
}

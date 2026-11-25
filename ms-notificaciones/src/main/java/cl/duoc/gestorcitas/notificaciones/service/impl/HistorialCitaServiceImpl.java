package cl.duoc.gestorcitas.notificaciones.service.impl;

import cl.duoc.gestorcitas.notificaciones.entity.HistorialCita;
import cl.duoc.gestorcitas.notificaciones.messaging.kafka.CitaEventoKafkaMsg;
import cl.duoc.gestorcitas.notificaciones.repository.HistorialCitaRepository;
import cl.duoc.gestorcitas.notificaciones.service.HistorialCitaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class HistorialCitaServiceImpl implements HistorialCitaService {

    private final HistorialCitaRepository repositorio;

    public HistorialCitaServiceImpl(HistorialCitaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional
    public boolean registrar(CitaEventoKafkaMsg evento, int particion, long offset) {
        if (repositorio.existsByEventoId(evento.eventoId())) {
            return false;
        }
        repositorio.save(HistorialCita.desde(evento, particion, offset, LocalDateTime.now()));
        return true;
    }
}

package cl.duoc.gestorcitas.citas.controller;

import cl.duoc.gestorcitas.citas.config.UsuarioActual;
import cl.duoc.gestorcitas.citas.dto.*;
import cl.duoc.gestorcitas.citas.entity.EstadoCita;
import cl.duoc.gestorcitas.citas.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Capa HTTP del microservicio de citas. Solo traduce HTTP <-> servicio.
 * Toda regla de negocio vive en {@link CitaService}.
 */
@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    public ResponseEntity<CitaResponse> reservar(@Valid @RequestBody CrearCitaRequest request,
                                                 @UsuarioActual UsuarioContexto usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.reservar(request, usuario));
    }

    @GetMapping("/mias")
    public List<CitaResponse> misCitas(@UsuarioActual UsuarioContexto usuario) {
        return citaService.listarMisCitas(usuario);
    }

    @PatchMapping("/{id}/cancelar")
    public CitaResponse cancelar(@PathVariable Long id, @UsuarioActual UsuarioContexto usuario) {
        return citaService.cancelar(id, usuario);
    }

    @GetMapping("/agenda")
    public List<CitaResponse> agenda(@RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                     @UsuarioActual UsuarioContexto usuario) {
        return citaService.listarAgenda(fecha, usuario);
    }

    @PatchMapping("/{id}/confirmar")
    public CitaResponse confirmar(@PathVariable Long id, @UsuarioActual UsuarioContexto usuario) {
        return citaService.confirmar(id, usuario);
    }

    @PatchMapping("/{id}/atender")
    public CitaResponse atender(@PathVariable Long id,
                                @Valid @RequestBody(required = false) AtenderCitaRequest request,
                                @UsuarioActual UsuarioContexto usuario) {
        return citaService.atender(id, request, usuario);
    }

    @GetMapping
    public List<CitaResponse> todas(@RequestParam(required = false) EstadoCita estado,
                                    @UsuarioActual UsuarioContexto usuario) {
        return citaService.listarTodas(estado, usuario);
    }

    @GetMapping("/resumen")
    public ResumenCitasResponse resumen(@UsuarioActual UsuarioContexto usuario) {
        return citaService.resumen(usuario);
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadResponse disponibilidad(@RequestParam Long medicoId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citaService.disponibilidad(medicoId, fecha);
    }
}

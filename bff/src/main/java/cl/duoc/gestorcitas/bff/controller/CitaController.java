package cl.duoc.gestorcitas.bff.controller;

import cl.duoc.gestorcitas.bff.dto.*;
import cl.duoc.gestorcitas.bff.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    public ResponseEntity<CitaResponse> reservar(@Valid @RequestBody CrearCitaRequest request,
                                                 @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.reservar(request, jwt));
    }

    @GetMapping("/mias")
    public List<CitaResponse> misCitas(@AuthenticationPrincipal Jwt jwt) {
        return citaService.misCitas(jwt);
    }

    @PatchMapping("/{id}/cancelar")
    public CitaResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return citaService.cancelar(id, jwt);
    }

    @GetMapping("/agenda")
    public List<CitaResponse> agenda(@RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                     @AuthenticationPrincipal Jwt jwt) {
        return citaService.agenda(fecha, jwt);
    }

    @PatchMapping("/{id}/confirmar")
    public CitaResponse confirmar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return citaService.confirmar(id, jwt);
    }

    @PatchMapping("/{id}/atender")
    public CitaResponse atender(@PathVariable Long id,
                                @Valid @RequestBody(required = false) AtenderCitaRequest request,
                                @AuthenticationPrincipal Jwt jwt) {
        return citaService.atender(id, request, jwt);
    }

    @GetMapping
    public List<CitaResponse> todas(@RequestParam(required = false) String estado,
                                    @AuthenticationPrincipal Jwt jwt) {
        return citaService.todas(estado, jwt);
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadResponse disponibilidad(@RequestParam Long medicoId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                                 @AuthenticationPrincipal Jwt jwt) {
        return citaService.disponibilidad(medicoId, fecha, jwt);
    }
}

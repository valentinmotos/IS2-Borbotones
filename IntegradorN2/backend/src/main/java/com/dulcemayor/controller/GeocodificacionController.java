package com.dulcemayor.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.dulcemayor.dto.ErrorRespuesta;
import com.dulcemayor.service.GeocodificacionService;
import com.dulcemayor.service.GeocodificacionService.Resultado;

@RestController
@RequestMapping("/api/geocodificacion")
public class GeocodificacionController {

    private final GeocodificacionService servicio;

    public GeocodificacionController(GeocodificacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Resultado> buscar(@RequestParam String q) {
        return servicio.buscar(q);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorRespuesta> errorDeBusqueda(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorRespuesta(ex.getReason(), null));
    }
}

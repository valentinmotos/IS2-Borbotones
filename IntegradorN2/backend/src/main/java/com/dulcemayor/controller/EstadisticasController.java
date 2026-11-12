package com.dulcemayor.controller;

import com.dulcemayor.dto.EstadisticasResponse;
import com.dulcemayor.service.EstadisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticasController {
    private final EstadisticasService estadisticas;

    @GetMapping
    public EstadisticasResponse consultar(@RequestParam int anio, @RequestParam(required = false) Integer mes) {
        return estadisticas.consultar(anio, mes);
    }
}

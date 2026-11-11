package com.dulcemayor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.MinoristaOpcion;
import com.dulcemayor.dto.PuntoDeVentaRequest;
import com.dulcemayor.dto.PuntoDeVentaResponse;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.MinoristaService;
import com.dulcemayor.service.PuntoDeVentaService;

import jakarta.validation.Valid;

/**
 * E2-06: puntos de venta. Consultan JEFE, ADMINISTRATIVO y PREVENTOR; solo el PREVENTOR da de
 * alta (reglas en SecurityConfig). El preventor del alta sale del token.
 */
@RestController
@RequestMapping("/api/puntos-de-venta")
public class PuntoDeVentaController {

    private final PuntoDeVentaService puntoService;
    private final MinoristaService minoristaService;

    public PuntoDeVentaController(PuntoDeVentaService puntoService, MinoristaService minoristaService) {
        this.puntoService = puntoService;
        this.minoristaService = minoristaService;
    }

    /** Puntos activos; con {@code departamentoId}, solo los de esa zona. */
    @GetMapping
    public List<PuntoDeVentaResponse> listar(@RequestParam(required = false) String departamentoId) {
        return puntoService.listarPuntoDeVentaActivo(departamentoId);
    }

    /** Minoristas activos para elegir el responsable del comercio. */
    @GetMapping("/minoristas")
    public List<MinoristaOpcion> minoristas() {
        return minoristaService.listarMinoristaActivo();
    }

    @GetMapping("/{id}")
    public PuntoDeVentaResponse buscar(@PathVariable String id) {
        return puntoService.buscarPuntoDeVenta(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PuntoDeVentaResponse crear(@Valid @RequestBody PuntoDeVentaRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return puntoService.crearPuntoDeVenta(request, usuario == null ? null : usuario.id());
    }
}

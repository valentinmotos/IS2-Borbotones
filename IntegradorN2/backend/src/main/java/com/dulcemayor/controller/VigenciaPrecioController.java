package com.dulcemayor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.VigenciaPrecioRequest;
import com.dulcemayor.dto.VigenciaPrecioResponse;
import com.dulcemayor.service.VigenciaPrecioService;

import jakarta.validation.Valid;

/**
 * E2-03: precios mayoristas de un producto. Las vigencias no se editan ni se borran, por eso no
 * extiende AbstractCrudController. Roles: JEFE y ADMINISTRATIVO (regla de /api/productos/** en
 * SecurityConfig).
 */
@RestController
@RequestMapping("/api/productos/{productoId}/precios")
public class VigenciaPrecioController {

    private final VigenciaPrecioService vigencias;

    public VigenciaPrecioController(VigenciaPrecioService vigencias) {
        this.vigencias = vigencias;
    }

    /** Historial de precios, del más nuevo al más viejo. */
    @GetMapping
    public List<VigenciaPrecioResponse> listar(@PathVariable String productoId) {
        return vigencias.listarVigenciaPrecioActivo(productoId);
    }

    /** Precio vigente (404 si el producto todavía no tiene precio). */
    @GetMapping("/vigente")
    public VigenciaPrecioResponse vigente(@PathVariable String productoId) {
        return vigencias.buscarVigenciaPrecioVigente(productoId);
    }

    /** Alta de un precio: cierra el vigente el día anterior a la nueva fecha desde. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VigenciaPrecioResponse crear(@PathVariable String productoId,
            @Valid @RequestBody VigenciaPrecioRequest request) {
        return vigencias.crearVigenciaPrecio(productoId, request);
    }
}

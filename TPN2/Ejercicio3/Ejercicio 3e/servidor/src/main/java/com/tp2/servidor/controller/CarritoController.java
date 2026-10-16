package com.tp2.servidor.controller;

import com.tp2.servidor.dto.AgregarArticuloRequest;
import com.tp2.servidor.dto.CrearCarritoRequest;
import com.tp2.servidor.dto.EditarDetalleRequest;
import com.tp2.servidor.entity.Carrito;
import com.tp2.servidor.service.CarritoService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carritos")
public class CarritoController extends BaseRestController<Carrito, CrearCarritoRequest, String> {
    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        super(carritoService);
        this.carritoService = carritoService;
    }

    @PostMapping("/{carritoId}/detalles")
    public Carrito agregarArticulo(@PathVariable String carritoId, @RequestBody AgregarArticuloRequest request) {
        return carritoService.agregar(carritoId, request.getArticuloId(), request.getCantidad());
    }

    @PutMapping("/{carritoId}/detalles/{detalleId}")
    public Carrito editarDetalle(@PathVariable String carritoId, @PathVariable String detalleId, @RequestBody EditarDetalleRequest request) {
        return carritoService.editar(carritoId, detalleId, request.getCantidad());
    }

    @PostMapping("/{carritoId}/finalizar")
    public Carrito finalizar(@PathVariable String carritoId) {
        return carritoService.finalizar(carritoId);
    }
}

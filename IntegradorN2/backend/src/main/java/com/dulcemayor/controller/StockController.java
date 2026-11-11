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

import com.dulcemayor.dto.AjusteStockRequest;
import com.dulcemayor.dto.MovimientoStockRequest;
import com.dulcemayor.dto.MovimientoStockResponse;
import com.dulcemayor.dto.StockResponse;
import com.dulcemayor.service.StockService;

import jakarta.validation.Valid;

/**
 * E2-02: stock y movimientos de almacén. Los movimientos no se editan ni se borran (un error se
 * corrige con un ajuste), por eso no extiende AbstractCrudController. Roles: JEFE y ADMINISTRATIVO
 * (regla de /api/stock/** en SecurityConfig).
 */
@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stock;

    public StockController(StockService stock) {
        this.stock = stock;
    }

    /** Saldo actual de cada producto activo. */
    @GetMapping
    public List<StockResponse> listar() {
        return stock.listarStock();
    }

    /** Saldo actual de un producto. */
    @GetMapping("/{productoId}")
    public StockResponse buscar(@PathVariable String productoId) {
        return stock.buscarStockPorProducto(productoId);
    }

    /** Historial de movimientos del producto, del más nuevo al más viejo. */
    @GetMapping("/{productoId}/movimientos")
    public List<MovimientoStockResponse> movimientos(@PathVariable String productoId) {
        return stock.listarMovimientos(productoId);
    }

    /** Ingreso de mercadería (reposición). */
    @PostMapping("/ingresos")
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponse ingresar(@Valid @RequestBody MovimientoStockRequest request) {
        return stock.reponer(request.productoId(), request.cantidad(), request.observacion());
    }

    /** Ajuste manual del saldo, en más o en menos. */
    @PostMapping("/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponse ajustar(@Valid @RequestBody AjusteStockRequest request) {
        return stock.ajustar(request.productoId(), request.diferencia(), request.observacion());
    }
}

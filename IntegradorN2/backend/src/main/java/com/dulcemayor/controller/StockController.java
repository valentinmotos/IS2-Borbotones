package com.dulcemayor.controller;

import com.dulcemayor.dto.AjusteStockRequest;
import com.dulcemayor.dto.MovimientoStockRequest;
import com.dulcemayor.dto.MovimientoStockResponse;
import com.dulcemayor.dto.StockResponse;
import com.dulcemayor.service.StockService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    private final StockService stock;

    public StockController(StockService stock) { this.stock = stock; }

    @GetMapping
    public List<StockResponse> listar() { return stock.listar(); }

    @GetMapping("/{productoId}/movimientos")
    public List<MovimientoStockResponse> movimientos(@PathVariable String productoId) {
        return stock.listarMovimientos(productoId);
    }

    @PostMapping("/ingresos")
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponse ingresar(@Valid @RequestBody MovimientoStockRequest request) {
        return stock.ingresar(request);
    }

    @PostMapping("/ajustes")
    public StockResponse ajustar(@Valid @RequestBody AjusteStockRequest request) {
        return stock.ajustar(request);
    }
}

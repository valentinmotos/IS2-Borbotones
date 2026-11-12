package com.dulcemayor.controller;

import com.dulcemayor.dto.HistorialEstadoPedidoResponse;
import com.dulcemayor.service.PedidoService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidos;

    public PedidoController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('JEFE', 'ADMINISTRATIVO')")
    public List<HistorialEstadoPedidoResponse> historial(@PathVariable String id) {
        return pedidos.consultarHistorial(id);
    }
}

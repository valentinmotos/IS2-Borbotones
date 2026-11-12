package com.dulcemayor.controller;

import com.dulcemayor.dto.FacturaResponse;
import com.dulcemayor.dto.HistorialEstadoPedidoResponse;
import com.dulcemayor.dto.ResultadoFacturacionResponse;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.FacturacionService;
import com.dulcemayor.service.PedidoService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidos;
    private final FacturacionService facturacion;

    public PedidoController(PedidoService pedidos, FacturacionService facturacion) {
        this.pedidos = pedidos;
        this.facturacion = facturacion;
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('JEFE', 'ADMINISTRATIVO')")
    public List<HistorialEstadoPedidoResponse> historial(@PathVariable String id) {
        return pedidos.consultarHistorial(id);
    }

    /**
     * E4-04: procesa el pedido. Devuelve 200 tanto si queda FACTURADO como si queda SIN_STOCK
     * (ver {@code estadoPedido} y {@code faltantes}); el usuario responsable sale del token.
     */
    @PostMapping("/{id}/facturar")
    @PreAuthorize("hasAnyRole('JEFE', 'ADMINISTRATIVO')")
    public ResultadoFacturacionResponse facturar(@PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return facturacion.facturarPedido(id, usuario.id());
    }

    /** E4-04: procesa todos los pedidos CARGADO y SIN_STOCK, en orden de carga. */
    @PostMapping("/procesar")
    @PreAuthorize("hasAnyRole('JEFE', 'ADMINISTRATIVO')")
    public List<ResultadoFacturacionResponse> procesarPendientes(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return facturacion.procesarPedidosPendientes(usuario.id());
    }

    /** E4-04: factura del pedido (404 si todavía no se facturó). */
    @GetMapping("/{id}/factura")
    @PreAuthorize("hasAnyRole('JEFE', 'ADMINISTRATIVO')")
    public FacturaResponse factura(@PathVariable String id) {
        return facturacion.buscarFacturaPorPedido(id);
    }
}

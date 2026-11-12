package com.dulcemayor.controller;

import com.dulcemayor.dto.EntregaRequest;
import com.dulcemayor.dto.EntregaResponse;
import com.dulcemayor.dto.PedidoAlmacenResponse;
import com.dulcemayor.dto.PedidoListoRepartoResponse;
import com.dulcemayor.dto.RutaResponse;
import com.dulcemayor.dto.TomarPedidosRequest;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.DistribucionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * E5-02: portal móvil del distribuidor. La identidad sale del token: cada distribuidor solo ve y entrega
 * los pedidos de su propia ruta.
 */
@RestController
@RequestMapping("/api/distribucion")
@PreAuthorize("hasRole('DISTRIBUIDOR')")
public class DistribucionController {
    private final DistribucionService distribucion;

    public DistribucionController(DistribucionService distribucion) {
        this.distribucion = distribucion;
    }

    /** Pedidos facturados y pagados que todavía nadie tomó para repartir. */
    @GetMapping("/pedidos-listos")
    public List<PedidoListoRepartoResponse> pedidosListos() {
        return distribucion.listarPedidosListos();
    }

    /** Toma los pedidos elegidos: pasan a EN_REPARTO y se suman a la ruta de hoy. */
    @PostMapping("/reparto")
    public RutaResponse tomar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody TomarPedidosRequest request) {
        return distribucion.tomarPedidos(usuario.id(), request);
    }

    /** Ruta de reparto de hoy. La posición (GPS del celular) es opcional. */
    @GetMapping("/ruta")
    public RutaResponse ruta(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Double latitud, @RequestParam(required = false) Double longitud) {
        return distribucion.obtenerRutaDelDia(usuario.id(), latitud, longitud);
    }

    /** Detalle (productos a entregar) de un pedido de la ruta de hoy. */
    @GetMapping("/pedidos/{id}")
    public PedidoAlmacenResponse pedido(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable String id) {
        return distribucion.buscarPedido(usuario.id(), id);
    }

    /** Botón «Entregado»: ENTREGADO en el historial, siguiente punto y aviso por correo al próximo. */
    @PostMapping("/pedidos/{id}/entrega")
    public EntregaResponse entregar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id,
            @Valid @RequestBody(required = false) EntregaRequest request) {
        return distribucion.entregar(usuario.id(), id, request);
    }
}

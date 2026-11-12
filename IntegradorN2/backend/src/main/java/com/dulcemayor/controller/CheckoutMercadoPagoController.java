package com.dulcemayor.controller;

import com.dulcemayor.dto.CheckoutPagoResponse;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.CheckoutMercadoPagoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inicia un checkout de una factura únicamente para el minorista que la tiene asignada. */
@RestController
@RequestMapping("/api/minorista/facturas")
public class CheckoutMercadoPagoController {
    private final CheckoutMercadoPagoService checkout;

    public CheckoutMercadoPagoController(CheckoutMercadoPagoService checkout) { this.checkout = checkout; }

    @PostMapping("/{facturaId}/checkout")
    public CheckoutPagoResponse crear(@PathVariable String facturaId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return checkout.crearCheckout(facturaId, usuario.id(), usuario.nombreUsuario());
    }

    @GetMapping("/{facturaId}/checkout")
    public CheckoutPagoResponse consultar(@PathVariable String facturaId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return checkout.consultarCheckout(facturaId, usuario.id());
    }
}

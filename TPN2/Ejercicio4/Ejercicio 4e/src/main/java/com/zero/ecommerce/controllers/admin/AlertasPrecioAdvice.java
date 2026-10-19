package com.zero.ecommerce.controllers.admin;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.zero.ecommerce.services.VigenciaPrecioService;

/** Expone el total de alertas a todas las pantallas del panel administrativo. */
@ControllerAdvice(basePackages = "com.zero.ecommerce.controllers.admin")
public class AlertasPrecioAdvice {

    private final VigenciaPrecioService vigenciaPrecioService;

    public AlertasPrecioAdvice(VigenciaPrecioService vigenciaPrecioService) {
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    @ModelAttribute("cantidadAlertasPrecio")
    public long cantidadAlertasPrecio() {
        return vigenciaPrecioService.contarProductosConPrecioVencido();
    }
}

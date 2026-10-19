package com.zero.ecommerce.dto;

import java.util.List;

/**
 * Modelo de la vidriera: agrupa los productos con el mismo nombre (uno por talle) en una sola card. El
 * representante es el primer talle según el orden del listado y es al que lleva la card.
 */
public record ModeloCatalogoDTO(
        ProductoCatalogoDTO representante,
        List<String> talles,
        String precioFormateado) {

    public String id() {
        return representante.id();
    }

    public String nombre() {
        return representante.nombre();
    }

    public String imagenId() {
        return representante.imagenId();
    }

    public boolean enOferta() {
        return representante.enOferta();
    }

    public boolean variosTalles() {
        return talles.size() > 1;
    }
}

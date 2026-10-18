package com.zero.ecommerce.dto;

/** Filtros compartidos por las pantallas publicas del catalogo. */
public record FiltroCatalogoDTO(Double precioMinimo, Double precioMaximo, String talle, String orden) {

    public static final String ORDEN_NOMBRE = "nombre";
    public static final String ORDEN_PRECIO_ASC = "precio-asc";
    public static final String ORDEN_PRECIO_DESC = "precio-desc";

    public String ordenSeguro() {
        return switch (orden == null ? "" : orden) {
            case ORDEN_PRECIO_ASC -> ORDEN_PRECIO_ASC;
            case ORDEN_PRECIO_DESC -> ORDEN_PRECIO_DESC;
            default -> ORDEN_NOMBRE;
        };
    }
}

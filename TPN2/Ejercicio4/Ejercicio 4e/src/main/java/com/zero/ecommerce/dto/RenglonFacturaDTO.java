package com.zero.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Un renglón de la factura en PDF. Cada propiedad es un campo ($F{...}) de reports/factura.jrxml.
 */
@Getter
@AllArgsConstructor
public class RenglonFacturaDTO {

    private String codigo;
    private String producto;
    private String talle;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
}

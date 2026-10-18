package com.zero.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un renglón de la tabla dinámica del formulario de compra a proveedor: producto, cantidad y precio de costo
 * unitario. Cantidad y precio pueden venir vacíos; el service avisa que son obligatorios.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleFacturaItemDTO {

    private String productoId;
    private Integer cantidad;
    private Double precioUnitario;
}

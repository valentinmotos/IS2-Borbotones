package com.zero.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una fila de la lista dinámica de contactos del formulario de proveedor. El id viene vacío en las filas
 * nuevas y con el id del contacto en las que ya existían, para modificarlo en lugar de crear otro.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactoItemDTO {

    public static final String CORREO = "CORREO";
    public static final String CELULAR = "CELULAR";
    public static final String FIJO = "FIJO";

    private String id;
    private String tipo; // CORREO, CELULAR o FIJO
    private String valor;
    private String tipoContacto; // un TipoContacto: EMPRESA, LABORAL o PERSONAL
    private String observacion;
}

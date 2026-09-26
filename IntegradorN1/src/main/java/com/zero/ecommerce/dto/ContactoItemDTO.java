package com.zero.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactoItemDTO {
    private String id;
    private String tipo; // "CORREO", "CELULAR", "FIJO"
    private String valor;
    private String tipoContacto; // "EMPRESA", "LABORAL", "PERSONAL"
    private String observacion;
}

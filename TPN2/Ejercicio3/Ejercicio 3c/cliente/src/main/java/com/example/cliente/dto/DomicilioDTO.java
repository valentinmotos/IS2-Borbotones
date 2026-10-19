package com.example.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DomicilioDTO extends BaseDTO {
    private String calle;
    private Integer numero;
    private LocalidadDTO localidad;
}

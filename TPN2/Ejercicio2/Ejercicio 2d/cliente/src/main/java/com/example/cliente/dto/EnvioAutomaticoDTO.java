package com.example.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnvioAutomaticoDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private String asunto;
    private String cuerpoHtml;
    private Boolean activo;
}

package com.example.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LibroDTO {
    private Long id;
    private String titulo;
    private Integer fecha;
    private String genero;
    private Integer paginas;
    private String autor;
    private PersonaDTO persona;
}

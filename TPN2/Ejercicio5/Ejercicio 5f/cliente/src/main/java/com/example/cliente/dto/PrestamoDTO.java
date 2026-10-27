package com.example.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrestamoDTO {
    private Long id;
    private LibroDTO libro;
    private PersonaDTO persona;
    private String fechaPrestamo;
    private String fechaDevolucion;
    private Boolean devuelto;
}

package com.example.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonaDTO extends BaseDTO {
    private String nombre;
    private String apellido;
    private Integer dni;
    private String email;
    private String fechaNacimiento;
    private DomicilioDTO domicilio;
}

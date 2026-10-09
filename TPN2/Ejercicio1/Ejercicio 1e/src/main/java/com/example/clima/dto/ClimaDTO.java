package com.example.clima.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClimaDTO {
    private String ciudad;
    private String pais;
    private Double temperatura;
    private Double sensacionTermica;
    private Integer humedad;
    private Double viento;
    private String descripcion;
    private String icono;
}

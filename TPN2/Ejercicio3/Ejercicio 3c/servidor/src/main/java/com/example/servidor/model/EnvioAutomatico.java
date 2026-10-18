package com.example.servidor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class EnvioAutomatico extends EntidadBase {
    private String nombre;
    private String tipo;
    private String asunto;

    @Column(length = 4000)
    private String cuerpoHtml;

    private Boolean activo = true;
}

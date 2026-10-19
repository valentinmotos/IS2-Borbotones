package com.example.servidor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Libro extends EntidadBase {
    private String titulo;
    private Integer fecha;
    private String genero;
    private Integer paginas;
    private String autor;

    @ManyToOne
    private Persona persona;
}

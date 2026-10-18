package com.example.servidor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import com.example.servidor.patterns.creational.LibroBuilder;

@Entity
@Getter
@Setter
public class Libro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private Integer fecha;
    private String genero;
    private Integer paginas;
    private String autor;

    private String tipo = "FISICO";
    private String formato;
    private String urlDescarga;

    public static LibroBuilder builder() {
        return new LibroBuilder();
    }

    @ManyToOne
    private Persona persona;
}

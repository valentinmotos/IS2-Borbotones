package com.borbotones.biblioteca.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Editorial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank private String nombre;
    private boolean alta = true;
    @OneToMany(mappedBy = "editorial") private List<Libro> libros = new ArrayList<>();
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isAlta() { return alta; }
    public void setAlta(boolean alta) { this.alta = alta; }
}
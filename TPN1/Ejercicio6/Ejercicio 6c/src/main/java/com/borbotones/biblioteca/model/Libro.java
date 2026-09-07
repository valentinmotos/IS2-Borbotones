package com.borbotones.biblioteca.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
public class Libro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank private String isbn;
    @NotBlank private String titulo;
    @Min(0) private int anio;
    @Min(1) private int ejemplares;
    private int ejemplaresPrestados;
    private boolean alta = true;
    @ManyToOne(optional = false) private Autor autor;
    @ManyToOne(optional = false) private Editorial editorial;
    @OneToOne(cascade = CascadeType.ALL) private Imagen imagen;
    public Long getId() { return id; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String v) { isbn = v; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String v) { titulo = v; }
    public int getAnio() { return anio; }
    public void setAnio(int v) { anio = v; }
    public int getEjemplares() { return ejemplares; }
    public void setEjemplares(int v) { ejemplares = v; }
    public int getEjemplaresPrestados() { return ejemplaresPrestados; }
    public void setEjemplaresPrestados(int v) { ejemplaresPrestados = v; }
    public boolean isAlta() { return alta; }
    public void setAlta(boolean v) { alta = v; }
    public Autor getAutor() { return autor; }
    public void setAutor(Autor v) { autor = v; }
    public Editorial getEditorial() { return editorial; }
    public void setEditorial(Editorial v) { editorial = v; }
    public Imagen getImagen() { return imagen; }
    public void setImagen(Imagen v) { imagen = v; }
    public int getDisponibles() { return ejemplares - ejemplaresPrestados; }
}
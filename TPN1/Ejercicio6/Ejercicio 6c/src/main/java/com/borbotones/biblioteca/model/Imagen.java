package com.borbotones.biblioteca.model;

import jakarta.persistence.*;

@Entity
public class Imagen {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String nombre;
    private String mime;
    @Lob @Basic(fetch = FetchType.LAZY) private byte[] contenido;
    @OneToOne(mappedBy = "imagen") private Libro libro;
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }
    public byte[] getContenido() { return contenido; }
    public void setContenido(byte[] contenido) { this.contenido = contenido; }
}
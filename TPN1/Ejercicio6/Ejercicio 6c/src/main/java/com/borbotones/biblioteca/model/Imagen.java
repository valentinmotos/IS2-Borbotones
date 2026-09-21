package com.borbotones.biblioteca.model;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
public class Imagen {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String nombre;
    private String mime;
    @Lob @Basic(fetch = FetchType.LAZY) @NotAudited private byte[] contenido;
    @OneToOne(mappedBy = "imagen") @NotAudited private Libro libro;
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }
    public byte[] getContenido() { return contenido; }
    public void setContenido(byte[] contenido) { this.contenido = contenido; }
}

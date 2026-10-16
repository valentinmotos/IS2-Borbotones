package com.borbotones.videojuegos.entities;

import org.springframework.format.annotation.DateTimeFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Date;

@Entity
@Table(name = "videojuegos")
public class Videojuego {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotEmpty(message = "{NotEmpty.Videojuego.titulo}")
    private String titulo;

    @Size(min = 5, max = 100, message = "La descripcion debe ser entre 5 y 100 caracteres")
    private String descripcion;

    private String imagen;

    @Min(value = 5, message = "El precio debe tener un minimo de 5")
    @Max(value = 10000, message = "El precio debe ser menor a 10000")
    private float precio;

    @Min(value = 1, message = "El stock debe tener un minimo de 1")
    @Max(value = 10000, message = "El stock debe ser menor a 10000")
    private short stock;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "No puede ser nulo la fecha")
    @PastOrPresent(message = "Debe ser igual o menor a la fecha de hoy")
    private Date fechaLanzamiento;

    private boolean activo = true;

    @NotNull(message = "Es requerido el estudio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_estudio", nullable = false)
    private Estudio estudio;

    @NotNull(message = "Es requerida la categoria")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_categoria", nullable = false)
    private Categoria categoria;

    public Videojuego() {
    }

    public Videojuego(long id, String titulo, String descripcion, String imagen, float precio, short stock, Date fechaLanzamiento, boolean activo, Estudio estudio, Categoria categoria) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.imagen = imagen;
        this.precio = precio;
        this.stock = stock;
        this.fechaLanzamiento = fechaLanzamiento;
        this.activo = activo;
        this.estudio = estudio;
        this.categoria = categoria;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public float getPrecio() {
        return precio;
    }

    public void setPrecio(float precio) {
        this.precio = precio;
    }

    public short getStock() {
        return stock;
    }

    public void setStock(short stock) {
        this.stock = stock;
    }

    public Date getFechaLanzamiento() {
        return fechaLanzamiento;
    }

    public void setFechaLanzamiento(Date fechaLanzamiento) {
        this.fechaLanzamiento = fechaLanzamiento;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Estudio getEstudio() {
        return estudio;
    }

    public void setEstudio(Estudio estudio) {
        this.estudio = estudio;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
}
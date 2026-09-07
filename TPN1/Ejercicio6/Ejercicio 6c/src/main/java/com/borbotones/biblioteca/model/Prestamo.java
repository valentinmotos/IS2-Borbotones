package com.borbotones.biblioteca.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Prestamo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private LocalDate fechaPrestamo = LocalDate.now();
    private LocalDate fechaDevolucion;
    private boolean alta = true;
    @ManyToOne(optional = false) private Libro libro;
    @ManyToOne(optional = false) private Usuario usuario;
    public Long getId() { return id; }
    public LocalDate getFechaPrestamo() { return fechaPrestamo; }
    public LocalDate getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(LocalDate v) { fechaDevolucion = v; }
    public boolean isAlta() { return alta; }
    public void setAlta(boolean v) { alta = v; }
    public Libro getLibro() { return libro; }
    public void setLibro(Libro v) { libro = v; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario v) { usuario = v; }
}
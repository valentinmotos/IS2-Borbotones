package com.example.servidor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Prestamo extends EntidadBase {
    @ManyToOne
    private Libro libro;

    @ManyToOne
    private Persona persona;

    private LocalDate fechaPrestamo;
    private LocalDate fechaDevolucion;
    private Boolean devuelto = false;
}

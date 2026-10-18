package com.example.servidor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Persona extends EntidadBase {
    private String nombre;
    private String apellido;
    private Integer dni;
    private String email;
    private LocalDate fechaNacimiento;

    @OneToOne
    @JoinColumn(name = "domicilio_id", unique = false)
    private Domicilio domicilio;
}

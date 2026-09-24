package com.borbotones.integrador1.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Departamento {

    @Id
    private String id;

    private String nombre;
    private boolean eliminado;

    @ManyToOne
    private Provincia provincia;
}
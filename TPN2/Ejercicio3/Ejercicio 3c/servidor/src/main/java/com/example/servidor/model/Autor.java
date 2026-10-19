package com.example.servidor.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Autor extends EntidadBase {
    private String nombre;
    private String apellido;
    private String biografia;
}

package com.example.servidor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import com.example.servidor.patterns.creational.AutorPrototype;

@Entity
@Getter
@Setter
public class Autor implements AutorPrototype {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellido;
    private String biografia;

    @Override
    public Autor clonar() {
        Autor copia = new Autor();
        copia.setNombre(nombre);
        copia.setApellido(apellido);
        copia.setBiografia(biografia);
        return copia;
    }
}

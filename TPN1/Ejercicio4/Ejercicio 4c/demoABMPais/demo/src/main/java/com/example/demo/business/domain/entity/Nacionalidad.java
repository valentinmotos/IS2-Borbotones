package com.example.demo.business.domain.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;



// @Entity: indica que la clase representa una entidad persistente que JPA puede mapear a una tabla de la base de datos.
@Entity
public class Nacionalidad implements Serializable {
    

    // @Id: indica que el atributo es el identificador unico y la clave primaria de la entidad.
    @Id
    private String id;
    
    private String nombre;
    private boolean eliminado;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }
}   

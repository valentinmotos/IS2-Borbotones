package com.example.demo.business.domain.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;


// @Entity: indica que la clase representa una entidad persistente que JPA puede mapear a una tabla de la base de datos.
@Entity
public class Pais implements Serializable {
    

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

    // @Override: indica que el metodo sobrescribe un metodo heredado de una clase o implementado desde una interfaz.
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    // @Override: indica que el metodo sobrescribe un metodo heredado de una clase o implementado desde una interfaz.
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Pais)) {
            return false;
        }
        Pais other = (Pais) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    // @Override: indica que el metodo sobrescribe un metodo heredado de una clase o implementado desde una interfaz.
    @Override
    public String toString() {
        return "com.example.demo.business.domain.entity.Pais[ id=" + id + " ]";
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

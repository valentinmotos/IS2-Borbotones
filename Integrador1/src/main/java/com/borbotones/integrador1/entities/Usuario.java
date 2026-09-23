package com.borbotones.integrador1.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @Column(length = 36)
    private String id;

    @NotBlank(message = "Debe indicar el nombre de usuario")
    @Size(max = 100, message = "El nombre de usuario no puede superar los 100 caracteres")
    @Column(nullable = false, unique = true, length = 100)
    private String nombreUsuario;

    @NotBlank(message = "Debe indicar la clave")
    @Size(max = 100, message = "La clave almacenada no puede superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String clave;

    @NotNull(message = "Debe indicar el rol")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolUsuario rol;

    @Column(nullable = false)
    private boolean eliminado;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }
}

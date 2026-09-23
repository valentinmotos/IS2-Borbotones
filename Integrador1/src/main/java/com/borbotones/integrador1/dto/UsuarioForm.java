package com.borbotones.integrador1.dto;

import com.borbotones.integrador1.entities.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UsuarioForm {

    private String id;

    @NotBlank(message = "Debe indicar el nombre de usuario")
    @Size(max = 100, message = "El nombre de usuario no puede superar los 100 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "Debe indicar la clave")
    @Size(min = 6, max = 72, message = "La clave debe tener entre 6 y 72 caracteres")
    private String clave;

    @NotNull(message = "Debe indicar el rol")
    private RolUsuario rol;

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
}

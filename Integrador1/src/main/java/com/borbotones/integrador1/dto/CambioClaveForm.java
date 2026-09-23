package com.borbotones.integrador1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CambioClaveForm {

    @NotBlank(message = "Debe indicar la clave actual")
    private String claveActual;

    @NotBlank(message = "Debe indicar la nueva clave")
    @Size(min = 6, max = 72, message = "La clave debe tener entre 6 y 72 caracteres")
    private String nuevaClave;

    @NotBlank(message = "Debe confirmar la nueva clave")
    private String confirmarClave;

    public String getClaveActual() {
        return claveActual;
    }

    public void setClaveActual(String claveActual) {
        this.claveActual = claveActual;
    }

    public String getNuevaClave() {
        return nuevaClave;
    }

    public void setNuevaClave(String nuevaClave) {
        this.nuevaClave = nuevaClave;
    }

    public String getConfirmarClave() {
        return confirmarClave;
    }

    public void setConfirmarClave(String confirmarClave) {
        this.confirmarClave = confirmarClave;
    }
}

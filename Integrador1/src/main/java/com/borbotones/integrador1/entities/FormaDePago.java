package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.List;

@Entity
public class FormaDePago {

    @Id
    private String id;

    private TipoPago tipoPago;

    private String observacion;

    private boolean eliminado;

    // Constructores
    public FormaDePago() {
    }

    // Getters y Setters
    public void crearFormaDePago(TipoPago tipoPago, String observacion) {}
    public void modificarFormaDePago(String id, TipoPago tipoPago, String observacion) {}
    public void eliminarFormaDePago(String id) {}
    public List<FormaDePago> listarFormaDePago() {
        return List.of();
    }

    public List<FormaDePago> listarFormaDePagoActivo() {
        return List.of();
    }
}

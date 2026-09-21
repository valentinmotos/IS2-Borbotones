package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

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
    public void crearFormaDePago( tipoPago:TipoPago , obsevacion:String ){}
    public void modificarFormaDePago( id:String, tipoPago:TipoPago, obsevacion:String ){}
    public void eliminarFormaDePago( id:String ){}
    public List<FormaDePago> listarFormaDePago(){}
    public List<FormaDePago> listarFormaDePagoActivo(){}
}

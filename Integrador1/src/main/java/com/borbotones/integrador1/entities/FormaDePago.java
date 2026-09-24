package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class FormaDePago {

    @Id
    private String id;

    private TipoPago tipoPago;

    private String observacion;

    private boolean eliminado;

    // Constructores
    public FormaDePago() {
    }
}

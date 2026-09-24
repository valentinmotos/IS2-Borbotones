package com.borbotones.integrador1.entities;

import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Cliente extends Persona {

    private String direccionEstadia;

    @ManyToOne
    private Nacionalidad nacionalidad;

    @OneToMany
    private List<OrdenCompra> ordenesCompra;

    @OneToMany
    private List<FacturaCliente> facturas;
}

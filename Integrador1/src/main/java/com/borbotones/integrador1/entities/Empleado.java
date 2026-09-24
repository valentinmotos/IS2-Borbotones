package com.borbotones.integrador1.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Empleado extends Persona {

    @Enumerated(EnumType.STRING)
    private TipoEmpleado tipoEmpleado;

    @OneToMany
    private List<FacturaCliente> facturas;
}
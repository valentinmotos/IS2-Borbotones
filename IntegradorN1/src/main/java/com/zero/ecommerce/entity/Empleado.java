package com.zero.ecommerce.entity;

import com.zero.ecommerce.entity.enums.TipoEmpleado;

import java.util.List;

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

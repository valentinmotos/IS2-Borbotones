package com.zero.ecommerce.entities;

import com.zero.ecommerce.entities.enums.TipoEmpleado;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Empleado extends Persona {

    @Enumerated(EnumType.STRING)
    private TipoEmpleado tipoEmpleado;
}

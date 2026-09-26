package com.zero.ecommerce.entities;

import com.zero.ecommerce.entities.enums.Sexo;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Cliente extends Persona {

    // Opcional y sin uso, según las decisiones de diseño.
    private String direccionEstadia;

    // No está en el diagrama, pero el enunciado lo pide en el perfil del cliente (E2-07).
    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    @ManyToOne
    private Nacionalidad nacionalidad;
}

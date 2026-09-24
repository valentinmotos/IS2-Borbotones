package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Cliente extends Persona {

    // Opcional y sin uso, según las decisiones de diseño.
    private String direccionEstadia;

    @ManyToOne
    private Nacionalidad nacionalidad;
}

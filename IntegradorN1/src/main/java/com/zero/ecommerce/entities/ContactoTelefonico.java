package com.zero.ecommerce.entities;

import com.zero.ecommerce.entities.enums.TipoTelefono;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ContactoTelefonico extends Contacto {

    private String telefono;

    @Enumerated(EnumType.STRING)
    private TipoTelefono tipoTelefono;
}

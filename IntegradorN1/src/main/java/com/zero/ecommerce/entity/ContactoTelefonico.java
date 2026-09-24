package com.zero.ecommerce.entity;

import com.zero.ecommerce.entity.enums.TipoTelefono;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ContactoTelefonico extends Contacto {

    private String telefono;

    private TipoTelefono tipoTelefono;
}

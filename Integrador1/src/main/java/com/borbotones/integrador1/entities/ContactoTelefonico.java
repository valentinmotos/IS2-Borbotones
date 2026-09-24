package com.borbotones.integrador1.entities;

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
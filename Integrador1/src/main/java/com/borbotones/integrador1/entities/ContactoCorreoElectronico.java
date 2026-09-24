package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ContactoCorreoElectronico extends Contacto {

    private String email;
}
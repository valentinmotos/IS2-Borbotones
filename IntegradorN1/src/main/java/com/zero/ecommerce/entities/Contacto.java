package com.zero.ecommerce.entities;

import com.zero.ecommerce.entities.enums.TipoContacto;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Contacto extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private TipoContacto tipoContacto;

    private String observacion;
}

package com.zero.ecommerce.entity;

import com.zero.ecommerce.entity.enums.TipoContacto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public class Contacto {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private TipoContacto tipoContacto;

    private String observacion;
    private boolean eliminado;
}

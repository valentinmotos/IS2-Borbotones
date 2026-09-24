package com.zero.ecommerce.entity;

import com.zero.ecommerce.entity.enums.TipoPago;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class FormaDePago {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private TipoPago tipoPago;

    private String observacion;
    private boolean eliminado;
}

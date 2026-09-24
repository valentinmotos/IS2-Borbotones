package com.zero.ecommerce.entities;

import com.zero.ecommerce.entities.enums.TipoPago;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class FormaDePago extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private TipoPago tipoPago;

    private String observacion;
}

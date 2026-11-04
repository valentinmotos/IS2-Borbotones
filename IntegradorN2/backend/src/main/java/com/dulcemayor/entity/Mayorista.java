package com.dulcemayor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mayoristas")
@Getter
@Setter
public class Mayorista extends BaseEntity {

    @Column(name = "razon_social", nullable = false, length = 160)
    private String razonSocial;

    @Column(nullable = false, length = 13)
    private String cuit;
}

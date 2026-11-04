package com.dulcemayor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "localidades")
@Getter
@Setter
public class Localidad extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "codigo_postal", nullable = false, length = 20)
    private String codigoPostal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;
}

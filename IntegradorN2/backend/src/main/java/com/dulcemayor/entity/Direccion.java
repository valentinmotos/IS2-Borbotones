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
@Table(name = "direcciones")
@Getter
@Setter
public class Direccion extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String calle;

    @Column(nullable = false, length = 30)
    private String numeracion;

    @Column(length = 120)
    private String barrio;

    @Column(name = "manzana_piso", length = 80)
    private String manzanaPiso;

    @Column(name = "casa_departamento", length = 80)
    private String casaDepartamento;

    @Column(length = 500)
    private String referencia;

    @Column(nullable = false)
    private double latitud;

    @Column(nullable = false)
    private double longitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "localidad_id", nullable = false)
    private Localidad localidad;
}

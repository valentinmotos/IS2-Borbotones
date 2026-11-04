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
@Table(name = "puntos_de_venta")
@Getter
@Setter
public class PuntoDeVenta extends BaseEntity {

    @Column(name = "nombre_comercio", nullable = false, length = 160)
    private String nombreComercio;

    // Se conserva el UUID del minorista como indica el UML; Minorista pertenece a E0-06.
    @Column(name = "minorista_id", nullable = false, length = 36)
    private String idMinorista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false)
    private Direccion direccion;

    // Se conserva el UUID del preventor como indica el UML; Preventor pertenece a E0-06.
    @Column(name = "preventor_id", nullable = false, length = 36)
    private String idPreventor;
}

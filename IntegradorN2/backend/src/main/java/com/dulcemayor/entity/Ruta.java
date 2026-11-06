package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.RutaEstado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rutas")
@Getter
@Setter
public class Ruta extends BaseEntity {
    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RutaEstado estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preventor_id")
    private Preventor preventor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribuidor_id")
    private Distribuidor distribuidor;
}

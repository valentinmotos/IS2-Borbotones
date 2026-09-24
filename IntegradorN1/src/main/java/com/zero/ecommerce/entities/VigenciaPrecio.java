package com.zero.ecommerce.entities;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/**
 * El precio vigente de un producto es la vigencia con fechaHasta nula.
 */
@Entity
@Getter
@Setter
public class VigenciaPrecio extends BaseEntity {

    private LocalDate fechaDesde;

    private LocalDate fechaHasta;

    private double precio;

    @ManyToOne
    private Producto producto;
}

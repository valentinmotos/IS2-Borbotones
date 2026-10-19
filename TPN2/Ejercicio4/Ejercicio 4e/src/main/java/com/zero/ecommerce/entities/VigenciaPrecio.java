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

    public boolean estaVigente(LocalDate fecha) {
        if (fecha == null || fechaDesde == null) {
            return false;
        }
        if (!fecha.isBefore(fechaDesde)) {
            if (fechaHasta == null) {
                return true;
            }
            return !fecha.isAfter(fechaHasta);
        }
        return false;
    }
}

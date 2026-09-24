package com.zero.ecommerce.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.zero.ecommerce.entities.enums.EstadoFactura;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Factura extends BaseEntity {

    private long numeroFactura;

    private LocalDate fechaFactura;

    private double totalPagado;

    @Enumerated(EnumType.STRING)
    private EstadoFactura estado;

    @ManyToOne
    private FormaDePago formaDePago;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleFactura> detalles = new ArrayList<>();

    /**
     * Signo con el que los detalles de esta factura mueven el stock: +1 entra, -1 sale.
     */
    public abstract int getSignoStock();
}

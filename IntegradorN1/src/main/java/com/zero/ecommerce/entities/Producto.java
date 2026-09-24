package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

/**
 * Cada talle es un producto distinto. El precio está en VigenciaPrecio y el stock en los
 * movimientos de Stock, según las decisiones de diseño.
 */
@Entity
@Getter
@Setter
public class Producto extends BaseEntity {

    private String codigo;

    private String nombre;

    private String descripcion;

    private String talle;

    private boolean enOferta;

    @ManyToOne
    private SubCategoria subCategoria;

    @OneToOne
    private Imagen imagen;
}

package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SubCategoria extends BaseEntity {

    private String nombre;

    @ManyToOne
    private Categoria categoria;
}

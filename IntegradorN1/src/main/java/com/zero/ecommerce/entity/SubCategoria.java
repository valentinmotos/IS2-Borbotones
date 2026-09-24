package com.zero.ecommerce.entity;

import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SubCategoria {

    @Id
    private String id;

    private String nombre;
    private boolean eliminado;

    @ManyToOne
    private Categoria categoria;

    @OneToMany
    private List<Producto> productos;
}
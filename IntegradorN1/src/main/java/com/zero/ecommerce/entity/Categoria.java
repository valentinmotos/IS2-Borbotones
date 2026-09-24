package com.zero.ecommerce.entity;

import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Categoria {

    @Id
    private String id;

    private String nombre;
    private boolean eliminado;

    @OneToMany
    private List<SubCategoria> subCategorias;
}
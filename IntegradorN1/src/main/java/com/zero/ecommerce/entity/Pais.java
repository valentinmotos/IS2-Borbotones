package com.zero.ecommerce.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Pais {

    @Id
    private String id;

    private String nombre;
    private boolean eliminado;
}
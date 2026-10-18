package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Nacionalidad extends BaseEntity {

    private String nombre;
}

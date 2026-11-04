package com.dulcemayor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "departamentos")
@Getter
@Setter
public class Departamento extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nombre;
}

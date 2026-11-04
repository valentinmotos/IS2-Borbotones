package com.dulcemayor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "empleados", uniqueConstraints = {
        @UniqueConstraint(name = "uk_empleados_legajo", columnNames = "legajo")
})
@Getter
@Setter
public abstract class Empleado extends Persona {

    @Column(nullable = false, length = 40)
    private String legajo;
}

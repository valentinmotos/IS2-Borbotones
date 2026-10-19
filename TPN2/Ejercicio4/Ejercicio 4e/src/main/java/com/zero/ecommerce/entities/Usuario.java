package com.zero.ecommerce.entities;

import com.zero.ecommerce.entities.enums.RolUsuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Usuario extends BaseEntity {

    // Es el correo de la persona.
    @Column(nullable = false, unique = true)
    private String nombreUsuario;

    private String clave;

    @Enumerated(EnumType.STRING)
    private RolUsuario rol;

    // Mientras tenga valor, la cuenta está inactiva y no puede iniciar sesión (RF02).
    private String codigoActivacion;
}

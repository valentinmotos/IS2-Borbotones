package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ConfiguracionCorreoEmpresa extends BaseEntity {

    private String correo;

    private String clave;

    private String puerto;

    private String smtp;

    private boolean tls;

    @OneToOne
    private Empresa empresa;
}

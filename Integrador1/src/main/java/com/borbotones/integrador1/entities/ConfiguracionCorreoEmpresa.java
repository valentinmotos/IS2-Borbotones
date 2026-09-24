package com.borbotones.integrador1.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ConfiguracionCorreoEmpresa {

    @Id
    private String id;

    private String correo;
    private String clave;
    private String puerto;
    private String smtp;
    private boolean tls;
    private boolean eliminado;

    @OneToOne
    private Empresa empresa;
}
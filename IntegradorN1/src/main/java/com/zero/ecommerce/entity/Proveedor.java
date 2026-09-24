package com.zero.ecommerce.entity;

import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Proveedor {

    @Id
    private String id;

    private String razonSocial;

    @OneToMany
    private List<Contacto> contactos;

    @OneToMany
    private List<FacturaProveedor> facturas;
}
package com.zero.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class FacturaProveedor extends Factura {

    @ManyToOne
    private Proveedor proveedor;
}

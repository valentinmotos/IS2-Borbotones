package com.borbotones.integrador1.entities;

import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Producto {

    @Id
    private String id;

    private String codigo;
    private String nombre;
    private String descripcion;
    private String talle;
    private boolean enOferta;
    private boolean eliminado;

    @ManyToOne
    private SubCategoria subCategoria;

    @OneToOne
    private Imagen imagen;

    @OneToMany
    private List<VigenciaPrecio> vigenciasPrecio;

    @OneToMany
    private List<DetalleCompra> detallesCompra;

    @OneToMany
    private List<DetalleFactura> detallesFactura;

    @OneToOne
    private Stock stock;
}

package com.zero.ecommerce.dto;

import com.zero.ecommerce.entities.Producto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del formulario de producto, sin la imagen: esa viaja como MultipartFile aparte.
 * categoriaId solo sirve para precargar el select en cascada; el producto guarda la subcategoría.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductoForm {

    private String codigo;
    private String nombre;
    private String descripcion;
    private String talle;
    private boolean enOferta;
    private String categoriaId;
    private String subCategoriaId;

    /** Arma el formulario de edición a partir de un producto guardado. */
    public static ProductoForm desde(Producto producto) {
        ProductoForm form = new ProductoForm();
        form.setCodigo(producto.getCodigo());
        form.setNombre(producto.getNombre());
        form.setDescripcion(producto.getDescripcion());
        form.setTalle(producto.getTalle());
        form.setEnOferta(producto.isEnOferta());
        if (producto.getSubCategoria() != null) {
            form.setSubCategoriaId(producto.getSubCategoria().getId());
            form.setCategoriaId(producto.getSubCategoria().getCategoria().getId());
        }
        return form;
    }
}

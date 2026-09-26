package com.zero.ecommerce.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos del formulario de nueva compra a proveedor (FacturaProveedor). */
@Getter
@Setter
@NoArgsConstructor
public class CompraProveedorForm {

    private String proveedorId;
    private String formaDePagoId;
    private List<DetalleFacturaItemDTO> detalles = new ArrayList<>();

    /** Formulario de alta: arranca con un renglón vacío. */
    public static CompraProveedorForm nuevo() {
        CompraProveedorForm form = new CompraProveedorForm();
        form.getDetalles().add(new DetalleFacturaItemDTO());
        return form;
    }
}

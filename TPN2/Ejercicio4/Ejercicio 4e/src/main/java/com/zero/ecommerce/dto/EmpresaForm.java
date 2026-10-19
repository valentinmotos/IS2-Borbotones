package com.zero.ecommerce.dto;

import com.zero.ecommerce.entities.Empresa;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del formulario de empresa, sin la dirección: esa viaja en un DireccionForm aparte
 * porque fragments/direccion usa sus propios nombres de campo (calle, localidadId, ...).
 */
@Getter
@Setter
@NoArgsConstructor
public class EmpresaForm {

    private String razonSocial;
    private String cuit;
    private String tipoSucursal;
    private String correo;
    private String telefono;
    private String tipoTelefono;

    /** Arma el formulario de edición a partir de una empresa guardada. */
    public static EmpresaForm desde(Empresa empresa) {
        EmpresaForm form = new EmpresaForm();
        form.setRazonSocial(empresa.getRazonSocial());
        form.setCuit(empresa.getCuit());
        form.setTipoSucursal(empresa.getTipoSucursal() == null ? null : empresa.getTipoSucursal().name());
        empresa.buscarCorreoActivo().ifPresent(c -> form.setCorreo(c.getEmail()));
        empresa.buscarTelefonoActivo().ifPresent(t -> {
            form.setTelefono(t.getTelefono());
            form.setTipoTelefono(t.getTipoTelefono() == null ? null : t.getTipoTelefono().name());
        });
        return form;
    }
}

package com.zero.ecommerce.dto;

import com.zero.ecommerce.entities.Direccion;
import com.zero.ecommerce.entities.Localidad;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos que envía y recibe fragments/direccion. Además de los campos de la dirección guarda
 * los ids de país, provincia y departamento, para precargar los selects en cascada al editar
 * y al volver al formulario después de un error de validación.
 */
@Getter
@Setter
@NoArgsConstructor
public class DireccionForm {

    private String calle;
    private String numeracion;
    private String barrio;
    private String manzanaPiso;
    private String casaDepartamento;
    private String referencia;
    private String paisId;
    private String provinciaId;
    private String departamentoId;
    private String localidadId;

    /** Arma el formulario de edición a partir de una dirección guardada. */
    public static DireccionForm desde(Direccion direccion) {
        DireccionForm form = new DireccionForm();
        form.setCalle(direccion.getCalle());
        form.setNumeracion(direccion.getNumeracion());
        form.setBarrio(direccion.getBarrio());
        form.setManzanaPiso(direccion.getManzanaPiso());
        form.setCasaDepartamento(direccion.getCasaDepartamento());
        form.setReferencia(direccion.getReferencia());
        Localidad localidad = direccion.getLocalidad();
        if (localidad != null) {
            form.setLocalidadId(localidad.getId());
            form.setDepartamentoId(localidad.getDepartamento().getId());
            form.setProvinciaId(localidad.getDepartamento().getProvincia().getId());
            form.setPaisId(localidad.getDepartamento().getProvincia().getPais().getId());
        }
        return form;
    }
}

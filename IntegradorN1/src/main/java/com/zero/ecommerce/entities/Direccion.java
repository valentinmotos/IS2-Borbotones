package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Direccion extends BaseEntity {

    private String calle;

    private String numeracion;

    private String barrio;

    private String manzanaPiso;

    private String casaDepartamento;

    private String referencia;

    @ManyToOne
    private Localidad localidad;

    /** Experto: la dirección en una línea, por ejemplo "San Martín 1250, Ciudad de Mendoza (5500), Mendoza". */
    public String describir() {
        StringBuilder texto = new StringBuilder(calle + " " + numeracion);
        if (localidad != null) {
            texto.append(", ").append(localidad.getNombre()).append(" (").append(localidad.getCodigoPostal())
                    .append("), ").append(localidad.getDepartamento().getProvincia().getNombre());
        }
        return texto.toString();
    }
}

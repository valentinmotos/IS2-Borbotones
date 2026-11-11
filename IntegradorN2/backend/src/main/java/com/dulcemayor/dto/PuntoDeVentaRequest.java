package com.dulcemayor.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * E2-06: alta de un punto de venta. La dirección llega con los campos del componente de E1-04
 * (localidad, calle, numeración, … y coordenadas). El preventor sale del token, no del body.
 */
public record PuntoDeVentaRequest(
        @NotBlank(message = "Ingresá el nombre del comercio.")
        @Size(max = 160, message = "El nombre del comercio admite hasta 160 caracteres.") String nombreComercio,
        @NotBlank(message = "Seleccioná el minorista responsable.") String minoristaId,
        @NotBlank(message = "Seleccioná la localidad.") String localidadId,
        @NotBlank(message = "Ingresá la calle.")
        @Size(max = 160, message = "La calle admite hasta 160 caracteres.") String calle,
        @NotBlank(message = "Ingresá la numeración.")
        @Size(max = 30, message = "La numeración admite hasta 30 caracteres.") String numeracion,
        @Size(max = 120, message = "El barrio admite hasta 120 caracteres.") String barrio,
        @Size(max = 80, message = "Manzana o piso admite hasta 80 caracteres.") String manzanaPiso,
        @Size(max = 80, message = "Casa o departamento admite hasta 80 caracteres.") String casaDepartamento,
        @Size(max = 500, message = "La referencia admite hasta 500 caracteres.") String referencia,
        @NotNull(message = "Marcá la ubicación del comercio en el mapa.")
        @DecimalMin(value = "-90.0", message = "La latitud no es válida.")
        @DecimalMax(value = "90.0", message = "La latitud no es válida.") Double latitud,
        @NotNull(message = "Marcá la ubicación del comercio en el mapa.")
        @DecimalMin(value = "-180.0", message = "La longitud no es válida.")
        @DecimalMax(value = "180.0", message = "La longitud no es válida.") Double longitud) {
}

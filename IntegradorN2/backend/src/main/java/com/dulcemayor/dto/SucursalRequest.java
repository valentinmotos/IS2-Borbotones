package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.TipoEmpresa;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta y modificación de sucursal, incluyendo su dirección para conservarla como un único flujo. */
public record SucursalRequest(
        @NotBlank(message = "El nombre de la sucursal es obligatorio.")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.") String nombre,
        @NotNull(message = "El tipo de empresa es obligatorio.") TipoEmpresa tipo,
        @NotBlank(message = "La empresa es obligatoria.") String idMayorista,
        @NotBlank(message = "La localidad es obligatoria.") String idLocalidad,
        @NotBlank(message = "La calle es obligatoria.") @Size(max = 160, message = "La calle no puede superar los 160 caracteres.") String calle,
        @NotBlank(message = "La numeración es obligatoria.") @Size(max = 30, message = "La numeración no puede superar los 30 caracteres.") String numeracion,
        @Size(max = 120, message = "El barrio no puede superar los 120 caracteres.") String barrio,
        @Size(max = 80, message = "El piso o manzana no puede superar los 80 caracteres.") String manzanaPiso,
        @Size(max = 80, message = "La casa o departamento no puede superar los 80 caracteres.") String casaDepartamento,
        @Size(max = 500, message = "La referencia no puede superar los 500 caracteres.") String referencia,
        @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90.")
        @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90.") double latitud,
        @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180.")
        @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180.") double longitud,
        @NotBlank(message = "La foto de la sucursal es obligatoria.")
        @Size(max = 36, message = "El identificador de imagen no es válido.") String idImagen) {
}

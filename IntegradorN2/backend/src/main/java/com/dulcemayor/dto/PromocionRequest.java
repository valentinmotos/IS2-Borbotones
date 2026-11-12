package com.dulcemayor.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Datos para crear o modificar una promoción por volumen o por producto destacado. */
public record PromocionRequest(
        @NotBlank(message = "El código es obligatorio.")
        @Size(max = 50, message = "El código no puede superar los 50 caracteres.") String codigo,
        @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.") String descripcion,
        @NotNull(message = "El porcentaje de descuento es obligatorio.")
        @DecimalMin(value = "0.01", message = "El descuento debe ser mayor que cero.")
        @DecimalMax(value = "100.00", message = "El descuento no puede superar el 100%.") Double porcentajeDescuento,
        @Positive(message = "La cantidad mínima debe ser mayor que cero.") Integer cantidadMinima,
        String idProducto,
        @NotNull(message = "La fecha desde es obligatoria.") LocalDate fechaDesde,
        @NotNull(message = "La fecha hasta es obligatoria.") LocalDate fechaHasta) {
}

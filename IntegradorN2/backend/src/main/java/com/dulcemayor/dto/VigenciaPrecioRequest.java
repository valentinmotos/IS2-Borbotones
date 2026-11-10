package com.dulcemayor.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** E2-03: alta de un precio mayorista desde una fecha. La vigencia anterior se cierra sola. */
public record VigenciaPrecioRequest(
        @NotNull(message = "Ingresá la fecha desde.") LocalDate fechaDesde,
        @NotNull(message = "Ingresá el precio.") @Positive(message = "El precio debe ser mayor que cero.")
        Double precio) {
}

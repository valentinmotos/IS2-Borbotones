package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.TipoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FormaDePagoRequest(
        @NotNull(message = "El tipo de pago es obligatorio.") TipoPago tipoPago,
        @Size(max = 255, message = "La observación no puede superar los 255 caracteres.") String observacion) {
}

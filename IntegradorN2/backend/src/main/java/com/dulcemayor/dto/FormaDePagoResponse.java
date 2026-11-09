package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.TipoPago;
public record FormaDePagoResponse(String id, TipoPago tipoPago, String observacion, boolean activo) {
}

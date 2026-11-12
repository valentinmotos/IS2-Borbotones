package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.MonedaPago;
import java.time.LocalDate;

public record PagoResponse(String id, String facturaId, String formaDePagoId, String formaDePago,
        double monto, MonedaPago moneda, Double cotizacionAplicada, double equivalenteEnPesos,
        LocalDate fechaPago, String referencia, String observacion) {
}

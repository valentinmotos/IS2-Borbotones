package com.dulcemayor.service;

import java.util.Optional;
import java.time.LocalDateTime;

/** Puerto de salida para obtener una cotización desde un proveedor externo. */
public interface CotizacionProvider {
    Optional<CotizacionExterna> obtenerCotizacionOficial();

    record CotizacionExterna(LocalDateTime fecha, double valorVenta) {
    }
}

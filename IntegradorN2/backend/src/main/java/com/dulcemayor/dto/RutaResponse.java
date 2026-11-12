package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.RutaEstado;
import java.time.LocalDate;
import java.util.List;

/**
 * E5-01: ruta con sus paradas y, si se calculó desde una posición, el próximo punto.
 *
 * @param siguiente         parada pendiente más cercana a la posición; null si no se pidió o no quedan pendientes
 * @param distanciaKm       distancia en línea recta hasta {@code siguiente}; null si no hay siguiente
 * @param recorridoSugerido ids de las paradas pendientes en el orden sugerido (vecino más cercano)
 */
public record RutaResponse(String id, LocalDate fecha, RutaEstado estado, String preventorId,
        String distribuidorId, List<ParadaRutaResponse> paradas, ParadaRutaResponse siguiente,
        Double distanciaKm, List<String> recorridoSugerido) {
}

package com.dulcemayor.dto;

/**
 * E5-01: parada de una ruta con la ubicación de su punto de venta.
 * {@code visitada} es true cuando la parada ya tiene un pedido asociado.
 */
public record ParadaRutaResponse(String id, int orden, String puntoDeVentaId, String nombreComercio,
        String calle, String numeracion, String localidad, double latitud, double longitud,
        boolean visitada, String pedidoId) {
}

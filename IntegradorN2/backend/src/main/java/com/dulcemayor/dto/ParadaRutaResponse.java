package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.EstadoPedido;

/**
 * E5-01: parada de una ruta con la ubicación de su punto de venta.
 * {@code visitada} es true cuando la parada ya se hizo: en la ruta del preventor, cuando tiene pedido;
 * en la ruta de reparto (E5-02), cuando su pedido fue entregado.
 *
 * <p>NOTA (E5-02): se agregaron {@code numeroPedido} y {@code estadoPedido} (null si la parada no tiene
 * pedido) para el portal del distribuidor. Son campos nuevos al final: el front del preventor no cambia.</p>
 */
public record ParadaRutaResponse(String id, int orden, String puntoDeVentaId, String nombreComercio,
        String calle, String numeracion, String localidad, double latitud, double longitud,
        boolean visitada, String pedidoId, Long numeroPedido, EstadoPedido estadoPedido) {
}

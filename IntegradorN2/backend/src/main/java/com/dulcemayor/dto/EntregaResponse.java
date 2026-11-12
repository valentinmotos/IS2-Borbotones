package com.dulcemayor.dto;

/**
 * E5-02: resultado de marcar un pedido como entregado.
 *
 * @param historial      registro ENTREGADO (fecha, usuario y transportista)
 * @param ruta           ruta de reparto actualizada, con el recorrido desde la posición o el comercio entregado
 * @param siguiente      próximo punto propuesto; null si no quedan entregas
 * @param avisoEnCaminoA comercio al que se le pidió el correo "tu pedido está en camino"; null si no
 *                       quedan entregas o si ese comercio ya había sido avisado
 */
public record EntregaResponse(HistorialEstadoPedidoResponse historial, RutaResponse ruta,
        ParadaRutaResponse siguiente, String avisoEnCaminoA) {
}

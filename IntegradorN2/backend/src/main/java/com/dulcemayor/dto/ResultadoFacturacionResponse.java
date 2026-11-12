package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.EstadoPedido;
import java.util.List;

/**
 * E4-04: resultado de procesar un pedido.
 * <ul>
 *   <li>Facturado: {@code estadoPedido = FACTURADO}, {@code factura} con los datos y {@code faltantes} vacío.</li>
 *   <li>Sin stock: {@code estadoPedido = SIN_STOCK}, {@code factura = null} y la lista de faltantes.</li>
 *   <li>Error (solo en el procesamiento masivo): {@code estadoPedido = null} y el motivo en {@code mensaje}.</li>
 * </ul>
 */
public record ResultadoFacturacionResponse(String pedidoId, Long numeroPedido, EstadoPedido estadoPedido,
        FacturaResponse factura, List<FaltanteStockDTO> faltantes, String mensaje) {

    public static ResultadoFacturacionResponse error(String pedidoId, String mensaje) {
        return new ResultadoFacturacionResponse(pedidoId, null, null, null, List.of(), mensaje);
    }
}

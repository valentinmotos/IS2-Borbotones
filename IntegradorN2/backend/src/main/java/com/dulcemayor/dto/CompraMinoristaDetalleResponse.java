package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.EstadoPedido;
import java.time.LocalDateTime;
import java.util.List;

/**
 * E4-08: detalle de una compra del minorista: el resumen del listado, el código de promoción,
 * las líneas del pedido y la línea de tiempo del envío (historial de estados de E4-01).
 */
public record CompraMinoristaDetalleResponse(CompraMinoristaResponse compra, String codigoPromocion,
        List<Linea> detalles, List<Evento> lineaDeTiempo) {

    /** Una línea del pedido. */
    public record Linea(String productoId, String codigo, String producto, String presentacion,
            int cantidad, double precioUnitario, double subtotal) {}

    /**
     * Un cambio de estado del pedido. NOTA (E4-08): versión reducida de
     * {@link HistorialEstadoPedidoResponse}: no expone al minorista el usuario interno que hizo el
     * cambio; solo el nombre del transportista (reparto y entrega).
     */
    public record Evento(EstadoPedido estado, LocalDateTime fechaHora, String observacion, String transportista) {}
}

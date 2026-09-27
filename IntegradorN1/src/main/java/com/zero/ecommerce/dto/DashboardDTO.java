package com.zero.ecommerce.dto;

import java.util.List;

/** Resumen ejecutivo del panel administrativo (E5-07). */
public record DashboardDTO(
        long ventasMesCantidad,
        double ventasMesMonto,
        double variacionMesAnterior,
        long pedidosPendientesPago,
        long pedidosPendientesEnvio,
        long productosStockMalo,
        long productosPrecioVencido,
        long clientesRegistrados,
        List<VentaMensualDTO> ventasUltimosSeisMeses,
        List<ProductoMasVendidoDTO> productosMasVendidos) {
}

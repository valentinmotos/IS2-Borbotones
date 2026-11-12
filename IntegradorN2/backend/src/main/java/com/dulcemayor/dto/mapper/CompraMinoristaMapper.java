package com.dulcemayor.dto.mapper;

import com.dulcemayor.dto.CompraMinoristaDetalleResponse;
import com.dulcemayor.dto.CompraMinoristaResponse;
import com.dulcemayor.entity.DetallePedido;
import com.dulcemayor.entity.Distribuidor;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.HistorialEstadoPedido;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.service.FacturacionService;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * E4-08: Pedido (+ Factura) → DTO de compras del minorista. Se usa dentro de una transacción
 * (punto de venta, detalle, producto y transportista son LAZY).
 */
@Mapper(componentModel = "spring", imports = FacturacionService.class)
public interface CompraMinoristaMapper {

    /** {@code factura} puede ser null (pedido todavía sin facturar): los campos de factura quedan en null. */
    @Mapping(target = "pedidoId", source = "pedido.id")
    @Mapping(target = "numeroPedido", source = "pedido.numeroPedido")
    @Mapping(target = "fechaHoraCarga", source = "pedido.fechaHoraCarga")
    @Mapping(target = "comercio", source = "pedido.puntoDeVenta.nombreComercio")
    @Mapping(target = "subtotal", source = "pedido.subtotal")
    @Mapping(target = "descuento", source = "pedido.descuento")
    @Mapping(target = "total", source = "pedido.total")
    @Mapping(target = "estadoPedido", source = "pedido.estado")
    @Mapping(target = "facturaId", source = "factura.id")
    @Mapping(target = "numeroFactura", expression = "java(factura == null ? null"
            + " : FacturacionService.numeroVisible(factura.getNumeroFactura()))")
    @Mapping(target = "fechaFactura", source = "factura.fechaFactura")
    @Mapping(target = "estadoFactura", source = "factura.estado")
    @Mapping(target = "saldoPendiente", source = "saldoPendiente")
    @Mapping(target = "puedePagar", source = "puedePagar")
    CompraMinoristaResponse aCompra(Pedido pedido, Factura factura, BigDecimal saldoPendiente, boolean puedePagar);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "codigo", source = "producto.codigo")
    @Mapping(target = "producto", source = "producto.nombre")
    @Mapping(target = "presentacion", source = "producto.presentacion")
    CompraMinoristaDetalleResponse.Linea aLinea(DetallePedido detalle);

    List<CompraMinoristaDetalleResponse.Linea> aLineas(List<DetallePedido> detalles);

    @Mapping(target = "transportista", expression = "java(nombreCompleto(registro.getTransportista()))")
    CompraMinoristaDetalleResponse.Evento aEvento(HistorialEstadoPedido registro);

    List<CompraMinoristaDetalleResponse.Evento> aEventos(List<HistorialEstadoPedido> registros);

    default String nombreCompleto(Distribuidor transportista) {
        return transportista == null ? null : (transportista.getNombre() + " " + transportista.getApellido()).strip();
    }
}

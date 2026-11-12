package com.dulcemayor.dto.mapper;

import com.dulcemayor.dto.DetalleFacturaResponse;
import com.dulcemayor.dto.FacturaResponse;
import com.dulcemayor.entity.DetalleFactura;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.service.FacturacionService;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** E4-04: Factura → DTO. Se usa dentro de una transacción (pedido y detalle son LAZY). */
@Mapper(componentModel = "spring", imports = FacturacionService.class)
public interface FacturaMapper {

    @Mapping(target = "numeroVisible", expression = "java(FacturacionService.numeroVisible(factura.getNumeroFactura()))")
    @Mapping(target = "subtotal", source = "pedido.subtotal")
    @Mapping(target = "descuento", source = "pedido.descuento")
    @Mapping(target = "pedidoId", source = "pedido.id")
    @Mapping(target = "numeroPedido", source = "pedido.numeroPedido")
    @Mapping(target = "comercio", source = "pedido.puntoDeVenta.nombreComercio")
    FacturaResponse aRespuesta(Factura factura);

    List<FacturaResponse> aRespuestas(List<Factura> facturas);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "codigo", source = "producto.codigo")
    @Mapping(target = "producto", source = "producto.nombre")
    @Mapping(target = "presentacion", source = "producto.presentacion")
    DetalleFacturaResponse aRespuesta(DetalleFactura detalle);
}

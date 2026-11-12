package com.dulcemayor.dto.mapper;

import com.dulcemayor.dto.ParadaRutaResponse;
import com.dulcemayor.entity.ParadaRuta;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** E5-01: parada de ruta → respuesta con la ubicación del punto de venta aplanada (E5-02 suma el pedido). */
@Mapper(componentModel = "spring")
public interface ParadaRutaMapper {
    @Mapping(target = "puntoDeVentaId", source = "puntoDeVenta.id")
    @Mapping(target = "nombreComercio", source = "puntoDeVenta.nombreComercio")
    @Mapping(target = "calle", source = "puntoDeVenta.direccion.calle")
    @Mapping(target = "numeracion", source = "puntoDeVenta.direccion.numeracion")
    @Mapping(target = "localidad", source = "puntoDeVenta.direccion.localidad.nombre")
    @Mapping(target = "latitud", source = "puntoDeVenta.direccion.latitud")
    @Mapping(target = "longitud", source = "puntoDeVenta.direccion.longitud")
    @Mapping(target = "visitada", expression = "java(!parada.estaPendiente())")
    @Mapping(target = "pedidoId", source = "pedido.id")
    @Mapping(target = "numeroPedido", source = "pedido.numeroPedido")
    @Mapping(target = "estadoPedido", source = "pedido.estado")
    ParadaRutaResponse aRespuesta(ParadaRuta parada);

    List<ParadaRutaResponse> aRespuestas(List<ParadaRuta> paradas);
}

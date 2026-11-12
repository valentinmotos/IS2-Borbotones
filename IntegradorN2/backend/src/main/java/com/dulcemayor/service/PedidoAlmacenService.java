package com.dulcemayor.service;

import com.dulcemayor.dto.FaltanteStockDTO;
import com.dulcemayor.dto.PagoResponse;
import com.dulcemayor.dto.PedidoAlmacenResponse;
import com.dulcemayor.dto.PedidoAlmacenResponse.Linea;
import com.dulcemayor.dto.PedidoAlmacenResponse.Resumen;
import com.dulcemayor.dto.mapper.FacturaMapper;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.PedidoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PedidoAlmacenService {
    private final PedidoRepository pedidos;
    private final FacturaRepository facturas;
    private final FacturaMapper mapper;
    private final FacturacionService facturacion;
    private final PagoService pagos;

    public PedidoAlmacenService(PedidoRepository pedidos, FacturaRepository facturas, FacturaMapper mapper,
            FacturacionService facturacion, PagoService pagos) {
        this.pedidos = pedidos;
        this.facturas = facturas;
        this.mapper = mapper;
        this.facturacion = facturacion;
        this.pagos = pagos;
    }

    public List<Resumen> listar(EstadoPedido estado) {
        return pedidos.listarParaAlmacen(estado).stream().map(PedidoAlmacenService::resumen).toList();
    }

    public PedidoAlmacenResponse buscar(String id) {
        Pedido pedido = pedidos.findDetalleByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el pedido."));
        var detalles = pedido.getDetalles().stream()
                .map(linea -> new Linea(linea.getId(), linea.getProducto().getNombre(), linea.getProducto().getCodigo(),
                        linea.getCantidad(), linea.getPrecioUnitario(), linea.getSubtotal()))
                .toList();
        List<FaltanteStockDTO> faltantes = FacturacionService.esProcesable(pedido.getEstado())
                ? facturacion.buscarFaltantes(pedido) : List.of();
        var factura = facturas.findByPedido_IdAndEliminadoFalse(id).map(mapper::aRespuesta).orElse(null);
        List<PagoResponse> pagosRegistrados = factura == null ? List.of() : pagos.listarPorFactura(factura.id());
        return new PedidoAlmacenResponse(resumen(pedido), pedido.getSubtotal(), pedido.getDescuento(),
                detalles, faltantes, factura, pagosRegistrados);
    }

    private static Resumen resumen(Pedido pedido) {
        return new Resumen(pedido.getId(), pedido.getNumeroPedido(), pedido.getPuntoDeVenta().getNombreComercio(),
                pedido.getFechaHoraCarga(), pedido.getEstado(), pedido.getTotal());
    }
}

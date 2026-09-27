package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.DashboardDTO;
import com.zero.ecommerce.dto.ReporteStockDTO;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.repositories.FacturaClienteRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private FacturaClienteRepository facturaClienteRepository;
    @Mock
    private OrdenCompraService ordenCompraService;
    @Mock
    private ClienteService clienteService;
    @Mock
    private ReporteStockService reporteStockService;
    @Mock
    private VigenciaPrecioService vigenciaPrecioService;

    @InjectMocks
    private DashboardService service;

    @Test
    void generarCalculaTotalesKPIsYTopProductos() throws Exception {
        FacturaCliente facturaActual = factura(LocalDate.now(), 3000.0, 1, "Remera Zero");
        FacturaCliente facturaActual2 = factura(LocalDate.now(), 1500.0, 2, "Gorra Zero");
        FacturaCliente facturaAnterior = factura(LocalDate.now().minusMonths(1), 5000.0, 1, "Remera Zero");

        when(facturaClienteRepository.findByEliminadoFalseOrderByFechaFacturaAsc())
                .thenReturn(List.of(facturaAnterior, facturaActual, facturaActual2));
        when(ordenCompraService.listarPedidoActivo()).thenReturn(List.of(
                orden(EstadoOrdenCompra.PENDIENTE_PAGO),
                orden(EstadoOrdenCompra.PENDIENTE_ENVIO),
                orden(EstadoOrdenCompra.ENTREGADO)));
        when(reporteStockService.generar()).thenReturn(new ReporteStockDTO("sede", "Zero", 0, 0, 0, 4, List.of()));
        when(vigenciaPrecioService.listarProductosConPrecioVencido()).thenReturn(List.of(
                nuevaAlerta("p-1"), nuevaAlerta("p-2")));
        when(clienteService.listarClienteActivo()).thenReturn(List.of(new Cliente(), new Cliente(), new Cliente()));

        DashboardDTO dashboard = service.generar();

        assertThat(dashboard.ventasMesCantidad()).isEqualTo(2L);
        assertThat(dashboard.ventasMesMonto()).isEqualTo(4500.0);
        assertThat(dashboard.variacionMesAnterior()).isEqualTo(-10.0);
        assertThat(dashboard.pedidosPendientesPago()).isEqualTo(1L);
        assertThat(dashboard.pedidosPendientesEnvio()).isEqualTo(1L);
        assertThat(dashboard.productosStockMalo()).isEqualTo(4);
        assertThat(dashboard.productosPrecioVencido()).isEqualTo(2L);
        assertThat(dashboard.clientesRegistrados()).isEqualTo(3L);
        assertThat(dashboard.ventasUltimosSeisMeses()).hasSize(6);
        assertThat(dashboard.productosMasVendidos()).hasSize(2);
        assertThat(dashboard.productosMasVendidos().get(0).cantidad()).isGreaterThanOrEqualTo(2L);
        assertThat(dashboard.productosMasVendidos().get(0).nombre()).isNotBlank();
    }

    private FacturaCliente factura(LocalDate fecha, double total, int cantidad, String nombreProducto) {
        FacturaCliente factura = new FacturaCliente();
        factura.setFechaFactura(fecha);
        factura.setEstado(EstadoFactura.PAGADA);
        factura.setTotalPagado(total);

        DetalleFactura detalle = new DetalleFactura();
        detalle.setCantidad(cantidad);
        detalle.setSubtotal(total);
        Producto producto = new Producto();
        producto.setId("prod-" + nombreProducto.replace(" ", "-").toLowerCase());
        producto.setNombre(nombreProducto);
        producto.setTalle("M");
        detalle.setProducto(producto);
        factura.setDetalles(List.of(detalle));
        return factura;
    }

    private OrdenCompra orden(EstadoOrdenCompra estado) {
        OrdenCompra orden = new OrdenCompra();
        orden.setEstadoOrdenCompra(estado);
        return orden;
    }

    private com.zero.ecommerce.dto.ProductoPrecioVencidoDTO nuevaAlerta(String productoId) {
        return new com.zero.ecommerce.dto.ProductoPrecioVencidoDTO(
                productoId,
                "COD-" + productoId,
                "Producto " + productoId,
                "M",
                1000.0,
                LocalDate.now().minusMonths(3),
                90,
                "cat-1",
                "Indumentaria",
                "Remeras");
    }
}

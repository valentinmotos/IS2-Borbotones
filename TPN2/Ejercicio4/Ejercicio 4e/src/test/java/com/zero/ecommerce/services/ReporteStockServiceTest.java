package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.ReporteStockDTO;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.Empresa;
import com.zero.ecommerce.entities.Factura;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Stock;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.enums.EstadoStock;

@ExtendWith(MockitoExtension.class)
class ReporteStockServiceTest {

    @Mock
    private StockService stockService;
    @Mock
    private EmpresaService empresaService;
    @InjectMocks
    private ReporteStockService service;

    @Test
    void generaReferenciaPorUltimaRecepcionExcluyeProductosSinRecepcionYSumaLosTotales() throws Exception {
        Empresa sede = new Empresa();
        sede.setId("sede");
        sede.setRazonSocial("Zero Sede Central");
        Producto malo = producto("p1", "REM-M", "Remera", "M");
        Producto bueno = producto("p2", "GOR-U", "Gorra", "U");
        Producto sinRecepcion = producto("p3", "ZAP-42", "Zapatilla", "42");

        // Orden real del repositorio: primero el movimiento más reciente de cada producto.
        List<Stock> movimientos = List.of(
                movimiento(malo, 3, new FacturaCliente(), 4),
                movimiento(bueno, 12, new FacturaProveedor(), 3),
                movimiento(sinRecepcion, 30, null, 2),
                movimiento(malo, 20, new FacturaProveedor(), 1));
        when(empresaService.buscarSedeCentral()).thenReturn(sede);
        when(stockService.listarStockActivo()).thenReturn(movimientos);

        ReporteStockDTO reporte = service.generar();

        assertThat(reporte.sucursal()).isEqualTo("Zero Sede Central");
        assertThat(reporte.productos()).hasSize(2);
        assertThat(reporte.totalUnidades()).isEqualTo(15);
        assertThat(reporte.cantidadBueno()).isEqualTo(1);
        assertThat(reporte.cantidadRegular()).isZero();
        assertThat(reporte.cantidadMalo()).isEqualTo(1);
        assertThat(reporte.cantidadBueno() + reporte.cantidadRegular() + reporte.cantidadMalo())
                .isEqualTo(reporte.cantidadProductos());
        assertThat(reporte.productos()).filteredOn(p -> p.productoId().equals("p1")).singleElement()
                .satisfies(p -> {
                    assertThat(p.stockActual()).isEqualTo(3);
                    assertThat(p.stockReferencia()).isEqualTo(20);
                    assertThat(p.porcentaje()).isEqualTo(15);
                    assertThat(p.estado()).isEqualTo(EstadoStock.MALO);
                });
    }

    @Test
    void respetaLosLimitesDeLosEstados() {
        assertThat(EstadoStock.desdePorcentaje(50)).isEqualTo(EstadoStock.REGULAR);
        assertThat(EstadoStock.desdePorcentaje(20)).isEqualTo(EstadoStock.REGULAR);
        assertThat(EstadoStock.desdePorcentaje(50.01)).isEqualTo(EstadoStock.BUENO);
        assertThat(EstadoStock.desdePorcentaje(19.99)).isEqualTo(EstadoStock.MALO);
    }

    private Producto producto(String id, String codigo, String nombre, String talle) {
        Categoria categoria = new Categoria();
        categoria.setId("cat");
        categoria.setNombre("Indumentaria");
        SubCategoria subCategoria = new SubCategoria();
        subCategoria.setNombre("Remeras");
        subCategoria.setCategoria(categoria);
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setTalle(talle);
        producto.setSubCategoria(subCategoria);
        return producto;
    }

    private Stock movimiento(Producto producto, int saldo, Factura factura, int minutos) {
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setCantidadActual(saldo);
        stock.setFecha(LocalDateTime.now().minusMinutes(minutos));
        if (factura != null) {
            DetalleFactura detalle = new DetalleFactura();
            detalle.setProducto(producto);
            detalle.setFactura(factura);
            stock.setDetalleFactura(detalle);
        }
        return stock;
    }
}

package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.DetalleVentaDTO;
import com.zero.ecommerce.dto.ReporteVentasDTO;
import com.zero.ecommerce.dto.SubtotalFormaPagoDTO;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaClienteRepository;

@ExtendWith(MockitoExtension.class)
class ReporteVentasServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 3, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 3, 31);

    @Mock
    private FacturaClienteRepository repository;
    @InjectMocks
    private ReporteVentasService service;

    @Test
    void rechazaRangoIncompletoOInvertidoSinConsultarElRepositorio() {
        assertThatThrownBy(() -> service.generar(null, HASTA))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("obligatorias");
        assertThatThrownBy(() -> service.generar(DESDE, null))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("obligatorias");
        assertThatThrownBy(() -> service.generar(HASTA, DESDE))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("no puede ser posterior");
        verifyNoInteractions(repository);
    }

    @Test
    void calculaComprasUnidadesMontoYOmiteDetallesEliminados() throws Exception {
        FormaDePago efectivo = formaPago("fp-efectivo", TipoPago.EFECTIVO, "Efectivo");
        FacturaCliente venta = venta("v1", 9, DESDE.plusDays(4), efectivo, "ORD-100");
        venta.agregarDetalle(producto("p1", "Remera", "M"), 3, 1000);
        DetalleFactura eliminado = venta.agregarDetalle(producto("p2", "Gorra", "U"), 2, 500);
        eliminado.setEliminado(true);
        when(repository.findByEstadoAndFechaFacturaBetweenAndEliminadoFalseOrderByFechaFacturaDescNumeroFacturaDesc(
                EstadoFactura.PAGADA, DESDE, HASTA)).thenReturn(List.of(venta));

        ReporteVentasDTO reporte = service.generar(DESDE, HASTA);

        assertThat(reporte.cantidadCompras()).isEqualTo(1);
        assertThat(reporte.unidadesVendidas()).isEqualTo(3);
        assertThat(reporte.montoTotal()).isEqualTo(3000);
        assertThat(reporte.detalle()).singleElement().satisfies(fila -> {
            assertThat(fila.producto()).isEqualTo("Remera (Talle M)");
            assertThat(fila.categoria()).isEqualTo("Indumentaria / General");
            assertThat(fila.identificadorCompra()).isEqualTo("ORD-100");
            assertThat(fila.formaPago()).isEqualTo("Efectivo");
        });
        assertThat(reporte.subtotalesPorFormaPago()).singleElement()
                .isEqualTo(new SubtotalFormaPagoDTO("Efectivo", 1, 3, 3000));
        verify(repository).findByEstadoAndFechaFacturaBetweenAndEliminadoFalseOrderByFechaFacturaDescNumeroFacturaDesc(
                EstadoFactura.PAGADA, DESDE, HASTA);
    }

    @Test
    void noMezclaFormasDePagoDistintasConLaMismaDescripcion() throws Exception {
        FormaDePago efectivo = formaPago("fp-1", TipoPago.EFECTIVO, "Pago en local");
        FormaDePago transferencia = formaPago("fp-2", TipoPago.TRANSFERENCIA, "Pago en local");
        FacturaCliente primera = venta("v1", 1, DESDE, efectivo, null);
        primera.agregarDetalle(producto("p1", "Remera", "M"), 1, 1000);
        FacturaCliente segunda = venta("v2", 2, DESDE.plusDays(1), transferencia, null);
        segunda.agregarDetalle(producto("p2", "Gorra", "U"), 2, 500);
        when(repository.findByEstadoAndFechaFacturaBetweenAndEliminadoFalseOrderByFechaFacturaDescNumeroFacturaDesc(
                EstadoFactura.PAGADA, DESDE, HASTA)).thenReturn(List.of(segunda, primera));

        ReporteVentasDTO reporte = service.generar(DESDE, HASTA);

        assertThat(reporte.cantidadCompras()).isEqualTo(2);
        assertThat(reporte.subtotalesPorFormaPago()).hasSize(2);
        assertThat(reporte.subtotalesPorFormaPago()).extracting(SubtotalFormaPagoDTO::cantidadCompras)
                .containsExactly(1, 1);
        assertThat(reporte.detalle()).extracting(DetalleVentaDTO::identificadorCompra)
                .containsExactly("Venta N.º 2", "Venta N.º 1");
    }

    private FacturaCliente venta(String id, long numero, LocalDate fecha, FormaDePago formaPago,
            String identificador) {
        FacturaCliente venta = new FacturaCliente();
        venta.setId(id);
        venta.setNumeroFactura(numero);
        venta.setFechaFactura(fecha);
        venta.setEstado(EstadoFactura.PAGADA);
        venta.setFormaDePago(formaPago);
        if (identificador != null) {
            OrdenCompra orden = new OrdenCompra();
            orden.setId("orden-" + id);
            orden.setIdentificadorCompra(identificador);
            venta.setOrdenCompra(orden);
        }
        return venta;
    }

    private FormaDePago formaPago(String id, TipoPago tipo, String observacion) {
        FormaDePago forma = new FormaDePago();
        forma.setId(id);
        forma.setTipoPago(tipo);
        forma.setObservacion(observacion);
        return forma;
    }

    private Producto producto(String id, String nombre, String talle) {
        Categoria categoria = new Categoria();
        categoria.setNombre("Indumentaria");
        SubCategoria subCategoria = new SubCategoria();
        subCategoria.setNombre("General");
        subCategoria.setCategoria(categoria);
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setTalle(talle);
        producto.setSubCategoria(subCategoria);
        return producto;
    }
}

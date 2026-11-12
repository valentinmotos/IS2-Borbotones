package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dulcemayor.dto.FaltanteStockDTO;
import com.dulcemayor.dto.ResultadoFacturacionResponse;
import com.dulcemayor.dto.StockResponse;
import com.dulcemayor.dto.mapper.FacturaMapperImpl;
import com.dulcemayor.entity.DetallePedido;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.PedidoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class FacturacionServiceTest {
    private static final String ID_PEDIDO = "pedido-1";
    private static final String ID_USUARIO = "usuario-1";

    @Mock private PedidoRepository pedidos;
    @Mock private FacturaRepository facturas;
    @Mock private StockService stock;
    @Mock private PedidoService estados;
    @Mock private ApplicationEventPublisher eventos;
    private FacturacionService service;
    private Producto alfajor;
    private Producto chupetin;

    @BeforeEach
    void setUp() {
        service = new FacturacionService(pedidos, facturas, stock, estados, new FacturaMapperImpl(), eventos,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
        alfajor = producto("prod-a", "ALF-001", "Alfajor");
        chupetin = producto("prod-b", "CHU-001", "Chupetín");
    }

    @Test
    void conStockDescuentaEmiteLaFacturaSecuencialYPasaAFacturado() {
        Pedido pedido = pedido(EstadoPedido.CARGADO);
        linea(pedido, alfajor, 3, 1000);
        linea(pedido, chupetin, 2, 500);
        linea(pedido, alfajor, 1, 1000);
        prepararPedido(pedido);
        saldo(alfajor, 10);
        saldo(chupetin, 2);
        when(facturas.ultimoNumeroFactura()).thenReturn(11L);
        when(facturas.saveAndFlush(any(Factura.class))).thenAnswer(inv -> {
            Factura factura = inv.getArgument(0);
            factura.setId("factura-1");
            return factura;
        });

        ResultadoFacturacionResponse resultado = service.facturarPedido(ID_PEDIDO, ID_USUARIO);

        assertThat(resultado.estadoPedido()).isEqualTo(EstadoPedido.FACTURADO);
        assertThat(resultado.faltantes()).isEmpty();
        assertThat(resultado.factura().numeroFactura()).isEqualTo(12L);
        assertThat(resultado.factura().numeroVisible()).isEqualTo("00000012");
        assertThat(resultado.factura().estado()).isEqualTo(EstadoFactura.PENDIENTE_PAGO);
        assertThat(resultado.factura().total()).isEqualTo(5000);
        assertThat(resultado.factura().detalles()).hasSize(3);
        // Las líneas del mismo producto se descuentan juntas.
        verify(stock).descontar(eq("prod-a"), eq(4), contains("00000012"));
        verify(stock).descontar(eq("prod-b"), eq(2), contains("00000012"));
        verify(estados).avanzar(eq(ID_PEDIDO), eq(EventoPedido.FACTURAR), eq(ID_USUARIO), contains("00000012"),
                isNull());
        ArgumentCaptor<FacturaEmitidaEvent> evento = ArgumentCaptor.forClass(FacturaEmitidaEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue()).isEqualTo(new FacturaEmitidaEvent("factura-1", ID_PEDIDO));
    }

    @Test
    void sinStockQuedaEnSinStockSinDescontarYDevuelveLosFaltantes() {
        Pedido pedido = pedido(EstadoPedido.CARGADO);
        linea(pedido, alfajor, 5, 1000);
        linea(pedido, chupetin, 1, 500);
        prepararPedido(pedido);
        saldo(alfajor, 2);
        saldo(chupetin, 9);

        ResultadoFacturacionResponse resultado = service.facturarPedido(ID_PEDIDO, ID_USUARIO);

        assertThat(resultado.estadoPedido()).isEqualTo(EstadoPedido.SIN_STOCK);
        assertThat(resultado.factura()).isNull();
        assertThat(resultado.faltantes()).containsExactly(new FaltanteStockDTO("prod-a", "Alfajor", 5, 2));
        verify(estados).avanzar(eq(ID_PEDIDO), eq(EventoPedido.MARCAR_SIN_STOCK), eq(ID_USUARIO),
                contains("Alfajor (pide 5, hay 2)"), isNull());
        verify(stock, never()).descontar(anyString(), anyInt(), anyString());
        verify(facturas, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void siYaEstabaSinStockYSigueSinStockNoRepiteElHistorial() {
        Pedido pedido = pedido(EstadoPedido.SIN_STOCK);
        linea(pedido, alfajor, 5, 1000);
        prepararPedido(pedido);
        saldo(alfajor, 0);

        ResultadoFacturacionResponse resultado = service.facturarPedido(ID_PEDIDO, ID_USUARIO);

        assertThat(resultado.estadoPedido()).isEqualTo(EstadoPedido.SIN_STOCK);
        verify(estados, never()).avanzar(anyString(), any(), anyString(), any(), any());
    }

    @Test
    void unProductoDadoDeBajaCuentaComoSinStock() {
        Pedido pedido = pedido(EstadoPedido.CARGADO);
        linea(pedido, alfajor, 1, 1000);
        prepararPedido(pedido);
        when(stock.buscarStockPorProducto("prod-a")).thenThrow(new RecursoNoEncontradoException("No se encontró."));

        assertThat(service.facturarPedido(ID_PEDIDO, ID_USUARIO).faltantes())
                .containsExactly(new FaltanteStockDTO("prod-a", "Alfajor", 1, 0));
    }

    @Test
    void rechazaPedidosQueNoEstanParaFacturar() {
        Pedido pedido = pedido(EstadoPedido.FACTURADO);
        when(pedidos.buscarActivoParaActualizar(ID_PEDIDO)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.facturarPedido(ID_PEDIDO, ID_USUARIO))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("FACTURADO", "no se puede facturar");
        verify(stock, never()).descontar(anyString(), anyInt(), anyString());
    }

    @Test
    void rechazaUnPedidoQueYaTieneFactura() {
        Pedido pedido = pedido(EstadoPedido.SIN_STOCK);
        when(pedidos.buscarActivoParaActualizar(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(facturas.findByPedido_IdAndEliminadoFalse(ID_PEDIDO)).thenReturn(Optional.of(new Factura()));

        assertThatThrownBy(() -> service.facturarPedido(ID_PEDIDO, ID_USUARIO))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("ya tiene una factura");
    }

    @Test
    void unPedidoInexistenteDa404() {
        when(pedidos.buscarActivoParaActualizar(ID_PEDIDO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.facturarPedido(ID_PEDIDO, ID_USUARIO))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unNumeroTomadoAlMismoTiempoSeInformaComoConflicto() {
        Pedido pedido = pedido(EstadoPedido.CARGADO);
        linea(pedido, alfajor, 1, 1000);
        prepararPedido(pedido);
        saldo(alfajor, 5);
        when(facturas.saveAndFlush(any(Factura.class))).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> service.facturarPedido(ID_PEDIDO, ID_USUARIO))
                .isInstanceOf(DuplicadoException.class).hasMessageContaining("Volvé a procesar");
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void procesarPendientesSigueAunqueUnPedidoFalle() {
        Pedido conStock = pedido(EstadoPedido.CARGADO);
        linea(conStock, alfajor, 1, 1000);
        Pedido roto = pedido(EstadoPedido.CARGADO);
        roto.setId("pedido-2");
        when(pedidos.findByEstadoInAndEliminadoFalseOrderByFechaHoraCargaAscIdAsc(any()))
                .thenReturn(List.of(conStock, roto));
        prepararPedido(conStock);
        when(pedidos.buscarActivoParaActualizar("pedido-2")).thenReturn(Optional.of(roto));
        saldo(alfajor, 5);
        when(facturas.saveAndFlush(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ResultadoFacturacionResponse> resultados = service.procesarPedidosPendientes(ID_USUARIO);

        assertThat(resultados).extracting(ResultadoFacturacionResponse::estadoPedido)
                .containsExactly(EstadoPedido.FACTURADO, null);
        assertThat(resultados.get(1).mensaje()).contains("no tiene productos");
    }

    private void prepararPedido(Pedido pedido) {
        when(pedidos.buscarActivoParaActualizar(pedido.getId())).thenReturn(Optional.of(pedido));
        when(facturas.findByPedido_IdAndEliminadoFalse(pedido.getId())).thenReturn(Optional.empty());
    }

    private void saldo(Producto producto, int cantidad) {
        when(stock.buscarStockPorProducto(producto.getId())).thenReturn(new StockResponse(producto.getId(),
                producto.getCodigo(), producto.getNombre(), producto.getPresentacion(), cantidad));
    }

    private static Pedido pedido(EstadoPedido estado) {
        Usuario usuario = new Usuario();
        usuario.setId(ID_USUARIO);
        Pedido pedido = new Pedido();
        LocalDateTime ahora = LocalDateTime.now();
        pedido.registrarCarga(usuario, ahora, null);
        switch (estado) {
            case SIN_STOCK -> pedido.avanzar(EventoPedido.MARCAR_SIN_STOCK, usuario, ahora, null, null);
            case FACTURADO -> pedido.avanzar(EventoPedido.FACTURAR, usuario, ahora, null, null);
            default -> { }
        }
        pedido.setId(ID_PEDIDO);
        pedido.setNumeroPedido(123456L);
        PuntoDeVenta punto = new PuntoDeVenta();
        punto.setNombreComercio("Kiosco Centro");
        pedido.setPuntoDeVenta(punto);
        return pedido;
    }

    private static void linea(Pedido pedido, Producto producto, int cantidad, double precio) {
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(precio);
        detalle.setSubtotal(cantidad * precio);
        pedido.agregarDetalle(detalle);
        pedido.setSubtotal(pedido.getSubtotal() + detalle.getSubtotal());
        pedido.setTotal(pedido.getSubtotal());
    }

    private static Producto producto(String id, String codigo, String nombre) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setPresentacion("Caja x24");
        return producto;
    }
}

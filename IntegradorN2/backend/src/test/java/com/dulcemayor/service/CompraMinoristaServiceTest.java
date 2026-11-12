package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dulcemayor.dto.ArchivoDescargable;
import com.dulcemayor.dto.CompraMinoristaDetalleResponse;
import com.dulcemayor.dto.CompraMinoristaResponse;
import com.dulcemayor.dto.mapper.CompraMinoristaMapperImpl;
import com.dulcemayor.entity.DetallePedido;
import com.dulcemayor.entity.Distribuidor;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.HistorialEstadoPedido;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.HistorialEstadoPedidoRepository;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** E4-08: compras del minorista. Criterio: ve todos sus pedidos y no puede ver los de otro. */
@ExtendWith(MockitoExtension.class)
class CompraMinoristaServiceTest {
    private static final String ID_USUARIO = "usuario-minorista";
    private static final String ID_MINORISTA = "minorista-1";

    @Mock private MinoristaRepository minoristas;
    @Mock private PedidoRepository pedidos;
    @Mock private FacturaRepository facturas;
    @Mock private HistorialEstadoPedidoRepository historial;
    @Mock private PagoService pagos;
    @Mock private FacturaPdfService pdf;
    private CompraMinoristaService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new CompraMinoristaService(minoristas, pedidos, facturas, historial, pagos, pdf,
                new CompraMinoristaMapperImpl());
        usuario = new Usuario();
        usuario.setId(ID_USUARIO);
        usuario.setNombreUsuario("minorista@dulcemayor.com.ar");
    }

    @Test
    void listaTodosSusPedidosConLaFacturaYSoloOfrecePagarLasImpagasConSaldo() {
        conMinorista();
        Pedido sinFacturar = pedido("p-1", 101, EstadoPedido.CARGADO);
        Pedido impago = pedido("p-2", 102, EstadoPedido.FACTURADO);
        Pedido pagado = pedido("p-3", 103, EstadoPedido.FACTURADO);
        Factura facturaImpaga = factura("f-2", 12, impago, EstadoFactura.PENDIENTE_PAGO);
        Factura facturaPagada = factura("f-3", 13, pagado, EstadoFactura.PAGADA);
        when(pedidos.findByPuntoDeVenta_Minorista_IdAndEliminadoFalseOrderByFechaHoraCargaDescIdDesc(ID_MINORISTA))
                .thenReturn(List.of(pagado, impago, sinFacturar));
        when(facturas.findByPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse(ID_MINORISTA))
                .thenReturn(List.of(facturaImpaga, facturaPagada));
        when(pagos.saldoPendienteEnPesos("f-2")).thenReturn(new BigDecimal("1500.00"));

        List<CompraMinoristaResponse> compras = service.listarPedidoPorMinorista(ID_USUARIO);

        assertThat(compras).extracting(CompraMinoristaResponse::numeroPedido).containsExactly(103L, 102L, 101L);
        CompraMinoristaResponse primera = compras.get(0);
        assertThat(primera.facturaId()).isEqualTo("f-3");
        assertThat(primera.numeroFactura()).isEqualTo("00000013");
        assertThat(primera.estadoFactura()).isEqualTo(EstadoFactura.PAGADA);
        assertThat(primera.saldoPendiente()).isEqualByComparingTo("0");
        assertThat(primera.puedePagar()).isFalse();

        CompraMinoristaResponse segunda = compras.get(1);
        assertThat(segunda.facturaId()).isEqualTo("f-2");
        assertThat(segunda.saldoPendiente()).isEqualByComparingTo("1500");
        assertThat(segunda.puedePagar()).isTrue();
        assertThat(segunda.comercio()).isEqualTo("Kiosco Centro");

        CompraMinoristaResponse tercera = compras.get(2);
        assertThat(tercera.facturaId()).isNull();
        assertThat(tercera.numeroFactura()).isNull();
        assertThat(tercera.saldoPendiente()).isNull();
        assertThat(tercera.puedePagar()).isFalse();
        verify(pagos, never()).saldoPendienteEnPesos("f-3");
    }

    @Test
    void unaFacturaImpagaSinSaldoNoOfrecePagar() {
        conMinorista();
        Pedido pedido = pedido("p-1", 101, EstadoPedido.FACTURADO);
        when(pedidos.findByPuntoDeVenta_Minorista_IdAndEliminadoFalseOrderByFechaHoraCargaDescIdDesc(ID_MINORISTA))
                .thenReturn(List.of(pedido));
        when(facturas.findByPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse(ID_MINORISTA))
                .thenReturn(List.of(factura("f-1", 1, pedido, EstadoFactura.PENDIENTE_PAGO)));
        when(pagos.saldoPendienteEnPesos("f-1")).thenReturn(BigDecimal.ZERO.setScale(2));

        assertThat(service.listarPedidoPorMinorista(ID_USUARIO).getFirst().puedePagar()).isFalse();
    }

    @Test
    void elDetalleTraeLasLineasYLaLineaDeTiempoSinExponerAlUsuarioInterno() {
        conMinorista();
        Pedido pedido = pedido("p-1", 101, EstadoPedido.FACTURADO);
        pedido.setCodigoPromocion("VERANO10");
        DetallePedido linea = new DetallePedido();
        Producto alfajor = new Producto();
        alfajor.setId("prod-a");
        alfajor.setCodigo("ALF-001");
        alfajor.setNombre("Alfajor");
        alfajor.setPresentacion("Caja x24");
        linea.setProducto(alfajor);
        linea.setCantidad(3);
        linea.setPrecioUnitario(1000);
        linea.setSubtotal(3000);
        pedido.agregarDetalle(linea);
        when(pedidos.findByIdAndPuntoDeVenta_Minorista_IdAndEliminadoFalse("p-1", ID_MINORISTA))
                .thenReturn(Optional.of(pedido));
        when(facturas.findByPedido_IdAndEliminadoFalse("p-1")).thenReturn(Optional.empty());
        Distribuidor transportista = new Distribuidor();
        transportista.setNombre("Dante");
        transportista.setApellido("Distribuidor");
        when(historial.findByPedido_IdAndEliminadoFalseOrderByFechaHoraAscIdAsc("p-1")).thenReturn(List.of(
                registro(pedido, EstadoPedido.CARGADO, "Pedido cargado.", null),
                registro(pedido, EstadoPedido.EN_REPARTO, "Salió a reparto.", transportista)));

        CompraMinoristaDetalleResponse detalle = service.buscarPedidoDelMinorista(ID_USUARIO, "p-1");

        assertThat(detalle.compra().numeroPedido()).isEqualTo(101L);
        assertThat(detalle.codigoPromocion()).isEqualTo("VERANO10");
        assertThat(detalle.detalles()).singleElement().satisfies(l -> {
            assertThat(l.codigo()).isEqualTo("ALF-001");
            assertThat(l.producto()).isEqualTo("Alfajor");
            assertThat(l.cantidad()).isEqualTo(3);
            assertThat(l.subtotal()).isEqualTo(3000);
        });
        assertThat(detalle.lineaDeTiempo()).extracting(CompraMinoristaDetalleResponse.Evento::estado)
                .containsExactly(EstadoPedido.CARGADO, EstadoPedido.EN_REPARTO);
        assertThat(detalle.lineaDeTiempo().get(0).transportista()).isNull();
        assertThat(detalle.lineaDeTiempo().get(1).transportista()).isEqualTo("Dante Distribuidor");
    }

    @Test
    void unPedidoDeOtroMinoristaResponde404() {
        conMinorista();
        when(pedidos.findByIdAndPuntoDeVenta_Minorista_IdAndEliminadoFalse("ajeno", ID_MINORISTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPedidoDelMinorista(ID_USUARIO, "ajeno"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el pedido.");
        verify(historial, never()).findByPedido_IdAndEliminadoFalseOrderByFechaHoraAscIdAsc(anyString());
    }

    @Test
    void descargaSuFacturaYNoLaDeOtroMinorista() {
        conMinorista();
        Pedido pedido = pedido("p-1", 101, EstadoPedido.FACTURADO);
        when(facturas.findByIdAndPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse("f-1", ID_MINORISTA))
                .thenReturn(Optional.of(factura("f-1", 1, pedido, EstadoFactura.PENDIENTE_PAGO)));
        when(facturas.findByIdAndPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse("ajena", ID_MINORISTA))
                .thenReturn(Optional.empty());
        when(pdf.nombreArchivo("f-1")).thenReturn("factura-00000001.pdf");
        when(pdf.generarFacturaPdf("f-1")).thenReturn("%PDF-".getBytes());

        ArchivoDescargable archivo = service.descargarFacturaDelMinorista(ID_USUARIO, "f-1");

        assertThat(archivo.nombreArchivo()).isEqualTo("factura-00000001.pdf");
        assertThat(archivo.contenido()).startsWith("%PDF-".getBytes());
        assertThatThrownBy(() -> service.descargarFacturaDelMinorista(ID_USUARIO, "ajena"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la factura.");
        verify(pdf, never()).generarFacturaPdf("ajena");
    }

    @Test
    void unUsuarioSinPerfilDeMinoristaResponde404() {
        when(minoristas.findByUsuario_IdAndEliminadoFalse(ID_USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPedidoPorMinorista(ID_USUARIO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró tu perfil de minorista.");
        assertThatThrownBy(() -> service.listarPedidoPorMinorista(" "))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private void conMinorista() {
        Minorista minorista = new Minorista();
        minorista.setId(ID_MINORISTA);
        minorista.setUsuario(usuario);
        when(minoristas.findByUsuario_IdAndEliminadoFalse(ID_USUARIO)).thenReturn(Optional.of(minorista));
    }

    private Pedido pedido(String id, long numero, EstadoPedido estado) {
        Pedido pedido = new Pedido();
        LocalDateTime ahora = LocalDateTime.now();
        pedido.registrarCarga(usuario, ahora, null);
        if (estado == EstadoPedido.FACTURADO) {
            pedido.avanzar(EventoPedido.FACTURAR, usuario, ahora, null, null);
        }
        pedido.setId(id);
        pedido.setNumeroPedido(numero);
        pedido.setSubtotal(3000);
        pedido.setTotal(3000);
        PuntoDeVenta punto = new PuntoDeVenta();
        punto.setNombreComercio("Kiosco Centro");
        pedido.setPuntoDeVenta(punto);
        return pedido;
    }

    private static Factura factura(String id, long numero, Pedido pedido, EstadoFactura estado) {
        Factura factura = new Factura();
        factura.setId(id);
        factura.setNumeroFactura(numero);
        factura.setFechaFactura(LocalDate.now());
        factura.setTotal(pedido.getTotal());
        factura.setEstado(estado);
        factura.setPedido(pedido);
        return factura;
    }

    private HistorialEstadoPedido registro(Pedido pedido, EstadoPedido estado, String observacion,
            Distribuidor transportista) {
        HistorialEstadoPedido registro = new HistorialEstadoPedido();
        registro.setPedido(pedido);
        registro.setEstado(estado);
        registro.setFechaHora(LocalDateTime.now());
        registro.setUsuario(usuario);
        registro.setObservacion(observacion);
        registro.setTransportista(transportista);
        return registro;
    }
}

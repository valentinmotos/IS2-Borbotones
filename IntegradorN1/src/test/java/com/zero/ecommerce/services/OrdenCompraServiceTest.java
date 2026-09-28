package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.PedidoDTO;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaClienteRepository;
import com.zero.ecommerce.repositories.OrdenCompraRepository;

@ExtendWith(MockitoExtension.class)
class OrdenCompraServiceTest {

    @Mock
    private OrdenCompraRepository repository;
    @Mock
    private FacturaClienteRepository facturaClienteRepository;

    @InjectMocks
    private OrdenCompraService service;

    private FormaDePago efectivo;
    private OrdenCompra carrito;
    private OrdenCompra pendientePago;
    private OrdenCompra entregado;

    @BeforeEach
    void setUp() {
        efectivo = new FormaDePago();
        efectivo.setId("fp-efectivo");
        efectivo.setObservacion("Efectivo");

        Cliente lucia = cliente("Lucía", "Gómez", "lucia.gomez@mail.com");
        Cliente martin = cliente("Martín", "Pérez", "martin.perez@mail.com");
        carrito = orden("o0", lucia, EstadoOrdenCompra.PENDIENTE_COMPLETAR, LocalDate.of(2026, 9, 20));
        pendientePago = orden("o1", lucia, EstadoOrdenCompra.PENDIENTE_PAGO, LocalDate.of(2026, 9, 18));
        entregado = orden("o2", martin, EstadoOrdenCompra.ENTREGADO, LocalDate.of(2026, 9, 1));
    }

    @Test
    void listarPedidoNoIncluyeLosCarritosAbiertos() throws Exception {
        when(repository.findByEliminadoFalseOrderByFechaDesc()).thenReturn(List.of(carrito, pendientePago, entregado));

        List<PedidoDTO> pedidos = service.listarPedido(null, null, null, null, null);

        assertThat(pedidos).extracting(PedidoDTO::id).containsExactly("o1", "o2");
    }

    @Test
    void listarPedidoFiltraPorEstado() throws Exception {
        when(repository.findByEliminadoFalseOrderByFechaDesc()).thenReturn(List.of(carrito, pendientePago, entregado));

        List<PedidoDTO> pedidos = service.listarPedido(EstadoOrdenCompra.ENTREGADO, null, null, null, null);

        assertThat(pedidos).extracting(PedidoDTO::id).containsExactly("o2");
    }

    @Test
    void listarPedidoFiltraPorClienteSinImportarMayusculasNiTildes() throws Exception {
        when(repository.findByEliminadoFalseOrderByFechaDesc()).thenReturn(List.of(pendientePago, entregado));

        assertThat(service.listarPedido(null, null, null, null, "LUCIA"))
                .extracting(PedidoDTO::cliente).containsExactly("Lucía Gómez");
        assertThat(service.listarPedido(null, null, null, null, "martin.perez@"))
                .extracting(PedidoDTO::id).containsExactly("o2");
    }

    @Test
    void listarPedidoFiltraPorNumeroSinImportarMayusculas() throws Exception {
        when(repository.findByEliminadoFalseOrderByFechaDesc()).thenReturn(List.of(pendientePago, entregado));

        assertThat(service.listarPedido(null, null, null, null, null, "ord-o2"))
                .extracting(PedidoDTO::id).containsExactly("o2");
        assertThat(service.listarPedido(null, null, null, null, null, "o1"))
                .extracting(PedidoDTO::id).containsExactly("o1");
    }

    @Test
    void listarPedidoFiltraPorFormaDePagoYMuestraLaDeLaFactura() throws Exception {
        when(repository.findByEliminadoFalseOrderByFechaDesc()).thenReturn(List.of(pendientePago, entregado));
        when(facturaClienteRepository.findByOrdenCompraIsNotNullAndEliminadoFalse())
                .thenReturn(List.of(factura(entregado, efectivo)));

        List<PedidoDTO> pedidos = service.listarPedido(null, "fp-efectivo", null, null, null);

        assertThat(pedidos).extracting(PedidoDTO::id).containsExactly("o2");
        assertThat(pedidos.get(0).formaDePago()).isEqualTo("Efectivo");
    }

    @Test
    void listarPedidoFiltraPorRangoDeFechas() throws Exception {
        when(repository.findByEliminadoFalseOrderByFechaDesc()).thenReturn(List.of(pendientePago, entregado));

        List<PedidoDTO> pedidos = service.listarPedido(null, null, LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 30), null);

        assertThat(pedidos).extracting(PedidoDTO::id).containsExactly("o1");
    }

    @Test
    void listarPedidoRechazaUnRangoDeFechasInvertido() {
        assertThatThrownBy(() -> service.listarPedido(null, null, LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 9, 1), null))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La fecha desde no puede ser posterior a la fecha hasta.");
    }

    @Test
    void contarPedidoPorEstadoIncluyeLosEstadosSinPedidosYNoCuentaElCarrito() {
        when(repository.findByEliminadoFalseOrderByFechaDesc()).thenReturn(List.of(carrito, pendientePago, entregado));

        assertThat(service.contarPedidoPorEstado()).containsExactly(
                entry("PENDIENTE_PAGO", 1L),
                entry("PENDIENTE_ENVIO", 0L),
                entry("PENDIENTE_ENTREGA", 0L),
                entry("ENTREGADO", 1L),
                entry("ANULADA", 0L));
    }

    @Test
    void buscarPedidoNoEncuentraUnCarritoAbierto() {
        when(repository.findById("o0")).thenReturn(Optional.of(carrito));

        assertThatThrownBy(() -> service.buscarPedido("o0"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El pedido no existe o fue eliminado.");
    }

    @Test
    void listarComprasClienteNoIncluyeSuCarritoAbierto() {
        when(repository.findByCliente_IdAndEliminadoFalseOrderByFechaDesc("c-lucia"))
                .thenReturn(List.of(carrito, pendientePago));

        assertThat(service.listarComprasCliente("c-lucia")).extracting(OrdenCompra::getId).containsExactly("o1");
        assertThat(service.listarComprasCliente(null)).isEmpty();
    }

    @Test
    void listarFilaCompraClienteTraeLaFormaDePagoDeLaFactura() {
        when(repository.findByCliente_IdAndEliminadoFalseOrderByFechaDesc("c-lucia"))
                .thenReturn(List.of(pendientePago));
        when(facturaClienteRepository.findByOrdenCompraIsNotNullAndEliminadoFalse())
                .thenReturn(List.of(factura(pendientePago, efectivo)));

        assertThat(service.listarFilaCompraCliente("c-lucia")).singleElement()
                .satisfies(fila -> {
                    assertThat(fila.formaDePago()).isEqualTo("Efectivo");
                    assertThat(fila.estado()).isEqualTo("PENDIENTE_PAGO");
                });
    }

    @Test
    void esCompraDelClienteSoloParaSuDuenio() {
        pendientePago.getCliente().setId("c-lucia");

        assertThat(service.esCompraDelCliente(pendientePago, "c-lucia")).isTrue();
        assertThat(service.esCompraDelCliente(pendientePago, "c-martin")).isFalse();
        assertThat(service.esCompraDelCliente(pendientePago, null)).isFalse();
    }

    @Test
    void convertirEstadoIgnoraElCarritoYLosValoresDesconocidos() {
        assertThat(service.convertirEstado("PENDIENTE_ENVIO")).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
        assertThat(service.convertirEstado("PENDIENTE_COMPLETAR")).isNull();
        assertThat(service.convertirEstado("cualquiera")).isNull();
    }

    private Cliente cliente(String nombre, String apellido, String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(correo);
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setUsuario(usuario);
        return cliente;
    }

    private OrdenCompra orden(String id, Cliente cliente, EstadoOrdenCompra estado, LocalDate fecha) {
        OrdenCompra orden = new OrdenCompra();
        orden.setId(id);
        orden.setIdentificadorCompra("ORD-" + id);
        orden.setCliente(cliente);
        orden.setEstadoOrdenCompra(estado);
        orden.setFecha(fecha);
        return orden;
    }

    private FacturaCliente factura(OrdenCompra orden, FormaDePago formaDePago) {
        FacturaCliente factura = new FacturaCliente();
        factura.setOrdenCompra(orden);
        factura.setFormaDePago(formaDePago);
        return factura;
    }
}

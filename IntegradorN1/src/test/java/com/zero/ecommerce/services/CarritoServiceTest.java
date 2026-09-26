package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.DetalleCompra;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ClienteRepository;
import com.zero.ecommerce.repositories.OrdenCompraRepository;
import com.zero.ecommerce.repositories.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private StockService stockService;
    @Mock
    private VigenciaPrecioService vigenciaPrecioService;

    @InjectMocks
    private CarritoService service;

    private Cliente cliente;
    private Producto producto;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId("c1");

        producto = new Producto();
        producto.setId("p1");
        producto.setNombre("Remera Zero");
        producto.setTalle("M");
    }

    @Test
    void obtenerCarritoCreaNuevoSiNoExiste() throws Exception {
        when(clienteRepository.existsByIdAndEliminadoFalse("c1")).thenReturn(true);
        when(clienteRepository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(cliente));
        when(ordenCompraRepository.findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
                "c1", EstadoOrdenCompra.PENDIENTE_COMPLETAR)).thenReturn(Optional.empty());
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdenCompra orden = service.obtenerCarrito("c1");

        assertThat(orden).isNotNull();
        assertThat(orden.getCliente()).isEqualTo(cliente);
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
        assertThat(orden.getTotal()).isZero();
    }

    @Test
    void agregarProductoCreaDetalleYCalculaTotal() throws Exception {
        when(clienteRepository.existsByIdAndEliminadoFalse("c1")).thenReturn(true);
        when(clienteRepository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(cliente));
        when(productoRepository.findByIdAndEliminadoFalse("p1")).thenReturn(Optional.of(producto));
        when(vigenciaPrecioService.buscarPrecioVigente("p1")).thenReturn(1500.0);
        when(stockService.buscarStockActual("p1")).thenReturn(10);

        OrdenCompra carrito = new OrdenCompra();
        carrito.setId("o1");
        carrito.setCliente(cliente);
        carrito.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_COMPLETAR);

        when(ordenCompraRepository.findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
                "c1", EstadoOrdenCompra.PENDIENTE_COMPLETAR)).thenReturn(Optional.of(carrito));

        DetalleCompra detalle = service.agregarProducto("c1", "p1", 2);

        assertThat(detalle.getProducto()).isEqualTo(producto);
        assertThat(detalle.getCantidad()).isEqualTo(2);
        assertThat(detalle.getSubtotal()).isEqualTo(3000.0);
        assertThat(carrito.getTotal()).isEqualTo(3000.0);
        verify(ordenCompraRepository).save(carrito);
    }

    @Test
    void agregarProductoExistenteSumaCantidadYRecalcula() throws Exception {
        when(clienteRepository.existsByIdAndEliminadoFalse("c1")).thenReturn(true);
        when(clienteRepository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(cliente));
        when(productoRepository.findByIdAndEliminadoFalse("p1")).thenReturn(Optional.of(producto));
        when(vigenciaPrecioService.buscarPrecioVigente("p1")).thenReturn(1500.0);
        when(stockService.buscarStockActual("p1")).thenReturn(10);

        OrdenCompra carrito = new OrdenCompra();
        carrito.setId("o1");
        carrito.setCliente(cliente);
        carrito.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
        carrito.crearDetalle(producto, 2, 1500.0);

        when(ordenCompraRepository.findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
                "c1", EstadoOrdenCompra.PENDIENTE_COMPLETAR)).thenReturn(Optional.of(carrito));

        DetalleCompra detalle = service.agregarProducto("c1", "p1", 3);

        assertThat(detalle.getCantidad()).isEqualTo(5);
        assertThat(detalle.getSubtotal()).isEqualTo(7500.0);
        assertThat(carrito.getTotal()).isEqualTo(7500.0);
    }

    @Test
    void rechazaAgregarSiSuperaStockDisponible() throws Exception {
        when(clienteRepository.existsByIdAndEliminadoFalse("c1")).thenReturn(true);
        when(clienteRepository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(cliente));
        when(productoRepository.findByIdAndEliminadoFalse("p1")).thenReturn(Optional.of(producto));
        when(vigenciaPrecioService.buscarPrecioVigente("p1")).thenReturn(1500.0);
        when(stockService.buscarStockActual("p1")).thenReturn(4);
        when(ordenCompraRepository.findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
                "c1", EstadoOrdenCompra.PENDIENTE_COMPLETAR)).thenReturn(Optional.of(new OrdenCompra()));

        assertThatThrownBy(() -> service.agregarProducto("c1", "p1", 5))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("No hay suficiente stock para agregar esa cantidad");
    }


    @Test
    void modificarCantidadActualizaSubtotalYTotal() throws Exception {
        when(clienteRepository.existsByIdAndEliminadoFalse("c1")).thenReturn(true);
        when(clienteRepository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(cliente));
        when(vigenciaPrecioService.buscarPrecioVigente("p1")).thenReturn(1000.0);
        when(stockService.buscarStockActual("p1")).thenReturn(20);

        OrdenCompra carrito = new OrdenCompra();
        carrito.setId("o1");
        carrito.setCliente(cliente);
        carrito.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
        DetalleCompra detalle = carrito.crearDetalle(producto, 2, 1000.0);
        detalle.setId("d1");

        when(ordenCompraRepository.findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
                "c1", EstadoOrdenCompra.PENDIENTE_COMPLETAR)).thenReturn(Optional.of(carrito));

        service.modificarCantidad("c1", "d1", 5);

        assertThat(detalle.getCantidad()).isEqualTo(5);
        assertThat(detalle.getSubtotal()).isEqualTo(5000.0);
        assertThat(carrito.getTotal()).isEqualTo(5000.0);
    }

    @Test
    void quitarProductoEliminaItemYRecalculaTotal() throws Exception {
        when(clienteRepository.existsByIdAndEliminadoFalse("c1")).thenReturn(true);
        when(clienteRepository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(cliente));

        OrdenCompra carrito = new OrdenCompra();
        carrito.setId("o1");
        carrito.setCliente(cliente);
        carrito.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
        DetalleCompra detalle = carrito.crearDetalle(producto, 2, 1000.0);
        detalle.setId("d1");

        when(ordenCompraRepository.findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
                "c1", EstadoOrdenCompra.PENDIENTE_COMPLETAR)).thenReturn(Optional.of(carrito));

        service.quitarProducto("c1", "d1");

        assertThat(carrito.getDetalles()).isEmpty();
        assertThat(carrito.getTotal()).isZero();
    }

    @Test
    void vaciarCarritoLimpiaTodosLosItems() throws Exception {
        when(clienteRepository.existsByIdAndEliminadoFalse("c1")).thenReturn(true);
        when(clienteRepository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(cliente));

        OrdenCompra carrito = new OrdenCompra();
        carrito.setId("o1");
        carrito.setCliente(cliente);
        carrito.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
        carrito.crearDetalle(producto, 2, 1000.0);

        when(ordenCompraRepository.findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
                "c1", EstadoOrdenCompra.PENDIENTE_COMPLETAR)).thenReturn(Optional.of(carrito));

        service.vaciarCarrito("c1");

        assertThat(carrito.getDetalles()).isEmpty();
        assertThat(carrito.getTotal()).isZero();
    }

    @Test
    void sincronizarAjustesReduceCantidadSiBajoStock() throws Exception {
        when(clienteRepository.existsByIdAndEliminadoFalse("c1")).thenReturn(true);
        when(clienteRepository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(cliente));
        when(vigenciaPrecioService.buscarPrecioVigente("p1")).thenReturn(1000.0);
        when(stockService.buscarStockActual("p1")).thenReturn(2); // Tenía 5, ahora hay 2

        OrdenCompra carrito = new OrdenCompra();
        carrito.setId("o1");
        carrito.setCliente(cliente);
        carrito.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
        DetalleCompra d1 = carrito.crearDetalle(producto, 5, 1000.0);
        d1.setId("d1");

        when(ordenCompraRepository.findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
                "c1", EstadoOrdenCompra.PENDIENTE_COMPLETAR)).thenReturn(Optional.of(carrito));

        var avisos = service.sincronizarAjustes("c1");

        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0)).contains("Se ajustó la cantidad");
        assertThat(d1.getCantidad()).isEqualTo(2);
        assertThat(d1.getSubtotal()).isEqualTo(2000.0);
        assertThat(carrito.getTotal()).isEqualTo(2000.0);
    }
}

package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.zero.ecommerce.dto.DetalleFacturaItemDTO;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaProveedorRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FacturaProveedorServiceTest {

    @Mock
    private FacturaProveedorRepository repository;
    @Mock
    private ProveedorService proveedorService;
    @Mock
    private FormaDePagoService formaDePagoService;
    @Mock
    private ProductoService productoService;
    @Mock
    private StockService stockService;
    @InjectMocks
    private FacturaProveedorService service;

    @BeforeEach
    void datos() throws ErrorServiceException {
        Proveedor proveedor = new Proveedor();
        proveedor.setId("prov");
        proveedor.setRazonSocial("Deportiva Cuyo");
        when(proveedorService.buscarProveedor("prov")).thenReturn(proveedor);
        FormaDePago formaDePago = new FormaDePago();
        formaDePago.setId("fp");
        when(formaDePagoService.buscarFormaDePago("fp")).thenReturn(formaDePago);
        for (String id : List.of("p1", "p2", "p3")) {
            Producto producto = new Producto();
            producto.setId(id);
            producto.setNombre("Producto " + id);
            producto.setTalle("M");
            when(productoService.buscarProducto(id)).thenReturn(producto);
        }
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void creaUnaCompraDeTresProductosPedidaYConElTotalDeLosSubtotales() throws ErrorServiceException {
        when(repository.findFirstByOrderByNumeroFacturaDesc()).thenReturn(Optional.of(compraNumero(7)));

        FacturaProveedor compra = service.crearFactura("prov", "fp", List.of(
                new DetalleFacturaItemDTO("p1", 20, 4500.0),
                new DetalleFacturaItemDTO("p2", 3, 1250.5),
                new DetalleFacturaItemDTO("p3", 1, 99.99)));

        assertThat(compra.getEstado()).isEqualTo(EstadoFactura.SIN_DEFINIR);
        assertThat(compra.getNumeroFactura()).isEqualTo(8);
        assertThat(compra.getFechaFactura()).isEqualTo(LocalDate.now());
        assertThat(compra.getDetalles()).extracting(DetalleFactura::getSubtotal)
                .containsExactly(90000.0, 3751.5, 99.99);
        assertThat(compra.getDetalles()).allMatch(d -> d.getFactura() == compra);
        assertThat(compra.getTotalPagado()).isCloseTo(93851.49, within(0.001));
        assertThat(compra.getDetalles().get(1).getPrecioUnitario()).isEqualTo(1250.5);
    }

    @Test
    void laPrimeraCompraTieneElNumeroUno() throws ErrorServiceException {
        when(repository.findFirstByOrderByNumeroFacturaDesc()).thenReturn(Optional.empty());
        FacturaProveedor compra = service.crearFactura("prov", "fp", List.of(new DetalleFacturaItemDTO("p1", 1, 10.0)));
        assertThat(compra.getNumeroFactura()).isEqualTo(1);
    }

    @Test
    void rechazaUnaCompraSinProductos() {
        // Los renglones vacíos o nulos (quitados del formulario) no cuentan como detalles.
        List<DetalleFacturaItemDTO> detalles = Arrays.asList(null, new DetalleFacturaItemDTO("", null, null));
        assertThatThrownBy(() -> service.crearFactura("prov", "fp", detalles))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La compra tiene que tener al menos un producto.");
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaUnProductoRepetido() {
        assertThatThrownBy(() -> service.crearFactura("prov", "fp", List.of(
                new DetalleFacturaItemDTO("p1", 2, 100.0),
                new DetalleFacturaItemDTO("p1", 3, 100.0))))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("está repetido");
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaCantidadesYPreciosQueNoSonMayoresACero() {
        assertThatThrownBy(() -> service.crearFactura("prov", "fp", List.of(new DetalleFacturaItemDTO("p1", 0, 100.0))))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La cantidad de Producto p1 (talle M) tiene que ser mayor a 0.");
        assertThatThrownBy(() -> service.crearFactura("prov", "fp", List.of(new DetalleFacturaItemDTO("p1", 5, -1.0))))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El precio de costo de Producto p1 (talle M) tiene que ser mayor a 0.");
        assertThatThrownBy(() -> service.crearFactura("prov", "fp", List.of(new DetalleFacturaItemDTO("p1", 5, null))))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("precio de costo");
        verify(repository, never()).save(any());
    }

    @Test
    void elProveedorYLaFormaDePagoSonObligatorios() {
        List<DetalleFacturaItemDTO> detalles = List.of(new DetalleFacturaItemDTO("p1", 1, 10.0));
        assertThatThrownBy(() -> service.crearFactura("", "fp", detalles))
                .isInstanceOf(ErrorServiceException.class).hasMessage("El proveedor es obligatorio.");
        assertThatThrownBy(() -> service.crearFactura("prov", null, detalles))
                .isInstanceOf(ErrorServiceException.class).hasMessage("La forma de pago es obligatoria.");
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() {
        assertThatThrownBy(() -> service.listarFacturaActivo(null, null, LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 1)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La fecha desde no puede ser posterior a la fecha hasta.");
    }

    @Test
    void recibirLaCompraLaPasaAPagadaYRegistraUnMovimientoPorDetalle() throws ErrorServiceException {
        FacturaProveedor compra = compraPedidaConDosDetalles();
        when(repository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(compra));

        service.recibirFactura("c1");

        assertThat(compra.getEstado()).isEqualTo(EstadoFactura.PAGADA);
        verify(stockService, times(2)).registrarMovimiento(any(DetalleFactura.class));
        verify(stockService).registrarMovimiento(compra.getDetalles().get(0));
        verify(stockService).registrarMovimiento(compra.getDetalles().get(1));
    }

    @Test
    void unaCompraRecibidaNoSePuedeRecibirDeNuevoNiAnular() throws ErrorServiceException {
        FacturaProveedor compra = compraPedidaConDosDetalles();
        compra.setEstado(EstadoFactura.PAGADA);
        when(repository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> service.recibirFactura("c1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La compra N.º 4 ya fue recibida: no se puede recibir de nuevo.");
        assertThatThrownBy(() -> service.anularFactura("c1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La compra N.º 4 ya fue recibida: no se puede anular.");
        verify(stockService, never()).registrarMovimiento(any());
    }

    @Test
    void anularUnaCompraPedidaNoMueveElStockYDespuesNoSePuedeRecibir() throws ErrorServiceException {
        FacturaProveedor compra = compraPedidaConDosDetalles();
        when(repository.findByIdAndEliminadoFalse("c1")).thenReturn(Optional.of(compra));

        service.anularFactura("c1");

        assertThat(compra.getEstado()).isEqualTo(EstadoFactura.ANULADA);
        assertThatThrownBy(() -> service.recibirFactura("c1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La compra N.º 4 está anulada: no se puede recibir.");
        verify(stockService, never()).registrarMovimiento(any());
    }

    private FacturaProveedor compraPedidaConDosDetalles() throws ErrorServiceException {
        FacturaProveedor compra = compraNumero(4);
        compra.setEstado(EstadoFactura.SIN_DEFINIR);
        compra.agregarDetalle(productoService.buscarProducto("p1"), 20, 4500);
        compra.agregarDetalle(productoService.buscarProducto("p2"), 5, 1000);
        return compra;
    }

    private FacturaProveedor compraNumero(long numero) {
        FacturaProveedor compra = new FacturaProveedor();
        compra.setNumeroFactura(numero);
        return compra;
    }
}

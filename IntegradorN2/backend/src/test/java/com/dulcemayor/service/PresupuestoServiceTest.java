package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dulcemayor.dto.CotizacionDolarResponse;
import com.dulcemayor.dto.ItemPresupuestoDTO;
import com.dulcemayor.dto.PresupuestoResponse;
import com.dulcemayor.dto.ProductoExcluidoDTO;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.Stock;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.exception.ServicioNoDisponibleException;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.StockRepository;
import com.dulcemayor.service.ChatbotProvider.ItemInterpretado;

/** E3-04: el backend valida lo que interpretó la IA contra catálogo, stock y precio, y calcula el total. */
@ExtendWith(MockitoExtension.class)
class PresupuestoServiceTest {

    private static final String PEDIDO = "quiero 10 alfajores y 5 chupetines";

    @Mock
    private ChatbotProvider chatbot;
    @Mock
    private ProductoRepository productos;
    @Mock
    private StockRepository stocks;
    @Mock
    private VigenciaPrecioService precios;
    @Mock
    private CotizacionDolarService cotizaciones;

    private PresupuestoService service;

    private final Producto alfajor = producto("p-alf", "ALF-001", "Alfajor de chocolate");
    private final Producto chupetin = producto("p-chu", "CAR-001", "Chupetín frutal");
    private final Producto chicle = producto("p-chi", "CHI-001", "Chicle de menta");

    @BeforeEach
    void setUp() {
        service = new PresupuestoService(chatbot, productos, stocks, precios, cotizaciones);
        lenient().when(productos.findByEliminadoFalse()).thenReturn(List.of(alfajor, chupetin, chicle));
        lenient().when(cotizaciones.buscarCotizacionVigente())
                .thenReturn(new CotizacionDolarResponse(LocalDate.of(2026, 11, 11), 1500));
    }

    private static Producto producto(String id, String codigo, String nombre) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setPresentacion("Caja x 12");
        return producto;
    }

    private void conStock(Producto producto, int cantidad) {
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setCantidadActual(cantidad);
        lenient().when(stocks.findByProducto_IdAndEliminadoFalse(producto.getId())).thenReturn(Optional.of(stock));
    }

    private void conPrecio(Producto producto, double precio) {
        lenient().when(precios.buscarPrecioVigente(producto.getId())).thenReturn(Optional.of(precio));
    }

    private void iaInterpreta(ItemInterpretado... items) {
        when(chatbot.interpretar(anyString(), anyList())).thenReturn(List.of(items));
    }

    @Test
    void criterioDeAceptacionCalculaElTotalYExcluyeLosProductosSinStock() {
        conStock(alfajor, 50);
        conPrecio(alfajor, 6000);
        conStock(chupetin, 0);
        conPrecio(chupetin, 3500);
        iaInterpreta(new ItemInterpretado("Alfajor de chocolate", 10), new ItemInterpretado("Chupetín frutal", 5));

        PresupuestoResponse presupuesto = service.armarPresupuesto(PEDIDO);

        assertThat(presupuesto.items()).containsExactly(new ItemPresupuestoDTO("p-alf", "ALF-001",
                "Alfajor de chocolate", "Caja x 12", 10, new BigDecimal("6000.00"), new BigDecimal("60000.00")));
        assertThat(presupuesto.excluidos()).containsExactly(
                new ProductoExcluidoDTO("Chupetín frutal", 5, PresupuestoService.MOTIVO_SIN_STOCK));
        assertThat(presupuesto.total()).isEqualByComparingTo("60000");
        assertThat(presupuesto.cotizacionDolar()).isEqualTo(1500);
        assertThat(presupuesto.fechaCotizacion()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(presupuesto.totalDolares()).isEqualByComparingTo("40.00");
    }

    @Test
    void conTodoEnStockSumaLosSubtotales() {
        conStock(alfajor, 50);
        conPrecio(alfajor, 6000);
        conStock(chupetin, 20);
        conPrecio(chupetin, 3500.5);
        iaInterpreta(new ItemInterpretado("Alfajor de chocolate", 10), new ItemInterpretado("Chupetín frutal", 5));

        PresupuestoResponse presupuesto = service.armarPresupuesto(PEDIDO);

        assertThat(presupuesto.excluidos()).isEmpty();
        assertThat(presupuesto.items()).extracting(ItemPresupuestoDTO::subtotal)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("60000"), new BigDecimal("17502.50"));
        assertThat(presupuesto.total()).isEqualByComparingTo("77502.50");
        assertThat(presupuesto.totalDolares()).isEqualByComparingTo("51.67");
    }

    @Test
    void lePasaALaIaLosNombresDeTodoElCatalogoActivo() {
        iaInterpreta();

        service.armarPresupuesto("  hola  ");

        org.mockito.Mockito.verify(chatbot).interpretar(eq("hola"),
                eq(List.of("Alfajor de chocolate", "Chupetín frutal", "Chicle de menta")));
    }

    @Test
    void excluyeConStockInsuficienteInformandoLoDisponible() {
        conStock(alfajor, 6);
        conPrecio(alfajor, 6000);
        iaInterpreta(new ItemInterpretado("Alfajor de chocolate", 10));

        PresupuestoResponse presupuesto = service.armarPresupuesto(PEDIDO);

        assertThat(presupuesto.items()).isEmpty();
        assertThat(presupuesto.excluidos()).containsExactly(
                new ProductoExcluidoDTO("Alfajor de chocolate", 10, "Stock insuficiente (disponibles: 6)."));
        assertThat(presupuesto.total()).isEqualByComparingTo("0");
    }

    @Test
    void productoSinRegistroDeStockCuentaComoSinStock() {
        when(stocks.findByProducto_IdAndEliminadoFalse("p-chi")).thenReturn(Optional.empty());
        iaInterpreta(new ItemInterpretado("Chicle de menta", 2));

        assertThat(service.armarPresupuesto("2 chicles").excluidos()).containsExactly(
                new ProductoExcluidoDTO("Chicle de menta", 2, PresupuestoService.MOTIVO_SIN_STOCK));
    }

    @Test
    void excluyeLosProductosSinPrecioVigente() {
        conStock(chicle, 30);
        when(precios.buscarPrecioVigente("p-chi")).thenReturn(Optional.empty());
        iaInterpreta(new ItemInterpretado("Chicle de menta", 2));

        assertThat(service.armarPresupuesto("2 chicles").excluidos()).containsExactly(
                new ProductoExcluidoDTO("Chicle de menta", 2, PresupuestoService.MOTIVO_SIN_PRECIO));
    }

    @Test
    void loQueNoEstaEnElCatalogoSeExcluyeConElNombreDelVisitante() {
        iaInterpreta(new ItemInterpretado("helado", 3));

        assertThat(service.armarPresupuesto("quiero 3 helados").excluidos()).containsExactly(
                new ProductoExcluidoDTO("helado", 3, PresupuestoService.MOTIVO_NO_ENCONTRADO));
    }

    @Test
    void aceptaNombresAproximadosSiCoincidenConUnSoloProducto() {
        conStock(alfajor, 50);
        conPrecio(alfajor, 6000);
        conStock(chupetin, 20);
        conPrecio(chupetin, 3500);
        iaInterpreta(new ItemInterpretado("alfajores", 1), new ItemInterpretado("CHUPETINES", 1));

        assertThat(service.armarPresupuesto(PEDIDO).items()).extracting(ItemPresupuestoDTO::idProducto)
                .containsExactly("p-alf", "p-chu");
    }

    @Test
    void unNombreQueCoincideConVariosProductosSeExcluyePorAmbiguo() {
        Producto otroAlfajor = producto("p-alf2", "ALF-002", "Alfajor de dulce de leche");
        when(productos.findByEliminadoFalse()).thenReturn(List.of(alfajor, otroAlfajor));
        iaInterpreta(new ItemInterpretado("alfajores", 4));

        assertThat(service.armarPresupuesto("4 alfajores").excluidos()).containsExactly(
                new ProductoExcluidoDTO("alfajores", 4, PresupuestoService.MOTIVO_AMBIGUO));
    }

    @Test
    void siLaIaRepiteUnProductoSeSumanLasCantidades() {
        conStock(alfajor, 50);
        conPrecio(alfajor, 6000);
        iaInterpreta(new ItemInterpretado("Alfajor de chocolate", 3), new ItemInterpretado("alfajor de chocolate", 2));

        assertThat(service.armarPresupuesto("3 alfajores y 2 alfajores más").items())
                .extracting(ItemPresupuestoDTO::cantidad).containsExactly(5);
    }

    @Test
    void sinCotizacionElPresupuestoSaleSoloEnPesos() {
        conStock(alfajor, 50);
        conPrecio(alfajor, 6000);
        when(cotizaciones.buscarCotizacionVigente()).thenThrow(new RecursoNoEncontradoException("sin cotización"));
        iaInterpreta(new ItemInterpretado("Alfajor de chocolate", 1));

        PresupuestoResponse presupuesto = service.armarPresupuesto("un alfajor");

        assertThat(presupuesto.total()).isEqualByComparingTo("6000");
        assertThat(presupuesto.cotizacionDolar()).isNull();
        assertThat(presupuesto.fechaCotizacion()).isNull();
        assertThat(presupuesto.totalDolares()).isNull();
    }

    @Test
    void mensajeVacioSeRechazaSinConsultarALaIa() {
        assertThatThrownBy(() -> service.armarPresupuesto("   "))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Escribí qué productos querés presupuestar.");
        verifyNoInteractions(chatbot);
    }

    @Test
    void siLaIaNoEstaDisponibleSePropagaElError() {
        when(chatbot.interpretar(anyString(), anyList()))
                .thenThrow(new ServicioNoDisponibleException("no disponible"));

        assertThatThrownBy(() -> service.armarPresupuesto(PEDIDO)).isInstanceOf(ServicioNoDisponibleException.class);
    }
}

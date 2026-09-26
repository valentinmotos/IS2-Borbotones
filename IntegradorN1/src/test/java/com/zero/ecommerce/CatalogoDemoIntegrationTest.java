package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Stock;
import com.zero.ecommerce.entities.enums.TipoImagen;
import com.zero.ecommerce.repositories.StockRepository;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.StockService;
import com.zero.ecommerce.services.SubCategoriaService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class CatalogoDemoIntegrationTest {

    private final MockMvc mvc;
    private final ProductoService productoService;
    private final SubCategoriaService subCategoriaService;
    private final StockService stockService;
    private final StockRepository stockRepository;

    CatalogoDemoIntegrationTest(@Autowired MockMvc mvc, @Autowired ProductoService productoService,
            @Autowired SubCategoriaService subCategoriaService, @Autowired StockService stockService,
            @Autowired StockRepository stockRepository) {
        this.mvc = mvc;
        this.productoService = productoService;
        this.subCategoriaService = subCategoriaService;
        this.stockService = stockService;
        this.stockRepository = stockRepository;
    }

    @Test
    void elSeederCargaVeinteProductosConImagenEnLasDoceSubcategorias() {
        List<Producto> productos = productoService.listarProductoActivo();
        assertThat(productos).hasSize(20);
        assertThat(productos).allSatisfy(p -> {
            assertThat(p.getImagen()).isNotNull();
            assertThat(p.getImagen().getTipoImagen()).isEqualTo(TipoImagen.PRODUCTO);
            assertThat(p.getImagen().getMime()).isEqualTo("image/jpeg");
            assertThat(p.getImagen().getContenido()).isNotEmpty();
            assertThat(stockService.buscarStockActual(p.getId())).isPositive();
        });
        // Cada producto tiene su propia imagen, aunque varios talles compartan la foto.
        assertThat(productos).extracting(p -> p.getImagen().getId()).doesNotHaveDuplicates();
        assertThat(productos).extracting(p -> p.getSubCategoria().getId())
                .containsAll(subCategoriaService.listarSubCategoriaActiva().stream().map(s -> s.getId()).toList());
        assertThat(productos).filteredOn(Producto::isEnOferta).isNotEmpty();
        // Un producto por talle: hay modelos con más de un talle.
        Map<String, Long> tallesPorModelo = productos.stream()
                .collect(Collectors.groupingBy(Producto::getNombre, Collectors.counting()));
        assertThat(tallesPorModelo.values()).anyMatch(cantidad -> cantidad > 1);
    }

    @Test
    void elPanelMuestraLosProductosConSusImagenesYStockCero() throws Exception {
        MockHttpSession sesion = login();
        mvc.perform(get("/admin/productos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("20 productos")))
                .andExpect(content().string(containsString("BIL-CUE-U")))
                .andExpect(content().string(containsString("/admin/productos?page=2")));
        // Orden por nombre: las zapatillas quedan en la segunda página.
        mvc.perform(get("/admin/productos").session(sesion).param("page", "2"))
                .andExpect(content().string(containsString("ZAP-RUN-42")));
        Producto zapatilla = productoService.buscarProductoPorCodigo("ZAP-RUN-42");
        mvc.perform(get("/imagen/" + zapatilla.getImagen().getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"));
    }

    @Test
    void elStockActualEsElSaldoDelUltimoMovimientoDelProducto() throws Exception {
        Producto remera = productoService.buscarProductoPorCodigo("REM-DRY-H-M");
        Producto gorra = productoService.buscarProductoPorCodigo("GOR-TRN-U");
        LocalDateTime ahora = LocalDateTime.now();
        guardarMovimiento(remera, 20, ahora.plusDays(1), false);
        guardarMovimiento(remera, 14, ahora.plusDays(2), false);
        guardarMovimiento(gorra, 50, ahora.plusDays(3), false);
        // Un movimiento dado de baja no cuenta, aunque sea el más reciente.
        guardarMovimiento(remera, 99, ahora.plusDays(4), true);

        assertThat(stockService.buscarStockActual(remera.getId())).isEqualTo(14);
        assertThat(stockService.buscarStockActual(gorra.getId())).isEqualTo(50);
        assertThat(stockService.listarStock()).extracting(Stock::getCantidadActual).startsWith(99, 50, 14, 20);
    }

    private void guardarMovimiento(Producto producto, int saldo, LocalDateTime fecha, boolean eliminado) {
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setCantidadActual(saldo);
        stock.setFecha(fecha);
        stock.setObservacion("Test");
        stock.setEliminado(eliminado);
        stockRepository.save(stock);
    }

    private MockHttpSession login() throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesion = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(sesion).param(token.getParameterName(), token.getToken())
                .param("username", "admin@zero.com.ar").param("password", "Admin123!"))
                .andExpect(redirectedUrl("/admin"));
        return sesion;
    }
}

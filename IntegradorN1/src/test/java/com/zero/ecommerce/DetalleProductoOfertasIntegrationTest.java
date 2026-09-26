package com.zero.ecommerce;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Stock;
import com.zero.ecommerce.repositories.StockRepository;
import com.zero.ecommerce.services.ProductoService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class DetalleProductoOfertasIntegrationTest {

    private final MockMvc mvc;
    private final ProductoService productoService;
    private final StockRepository stockRepository;

    DetalleProductoOfertasIntegrationTest(@Autowired MockMvc mvc, @Autowired ProductoService productoService,
            @Autowired StockRepository stockRepository) {
        this.mvc = mvc;
        this.productoService = productoService;
        this.stockRepository = stockRepository;
    }

    @Test
    void detalleMuestraDatosStockSelectorTallesYRelacionados() throws Exception {
        Producto remeraM = productoService.buscarProductoPorCodigo("REM-DRY-H-M");
        Producto remeraL = productoService.buscarProductoPorCodigo("REM-DRY-H-L");
        Producto shortRun = productoService.buscarProductoPorCodigo("SHO-RUN-H-M");
        guardarStock(remeraM, 7);
        guardarStock(remeraL, 3);
        guardarStock(shortRun, 5);

        mvc.perform(get("/producto/{id}", remeraM.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("publico/producto-detalle"))
                .andExpect(content().string(containsString("Remera Zero Dry Fit Hombre")))
                .andExpect(content().string(containsString("REM-DRY-H-M")))
                .andExpect(content().string(containsString("7 unidades disponibles")))
                .andExpect(content().string(containsString("max=\"7\"")))
                .andExpect(content().string(containsString("name=\"idProducto\"")))
                .andExpect(content().string(containsString("/cliente/carrito/agregar")))
                .andExpect(content().string(containsString("/producto/" + remeraL.getId())))
                .andExpect(content().string(containsString("Short Zero Run Hombre")));
    }

    @Test
    void detalleDevuelve404CuandoNoHayStockOElIdNoExiste() throws Exception {
        Producto sinStock = productoService.buscarProductoPorCodigo("BIL-CUE-U");
        guardarStock(sinStock, 0);

        mvc.perform(get("/producto/{id}", sinStock.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/producto/{id}", "id-inexistente")).andExpect(status().isNotFound());
    }

    @Test
    void ofertasSoloListaOfertasConStockYRespetaFiltros() throws Exception {
        Producto shortOferta = productoService.buscarProductoPorCodigo("SHO-RUN-H-M");
        Producto gorraOferta = productoService.buscarProductoPorCodigo("GOR-TRN-U");
        Producto remeraSinOferta = productoService.buscarProductoPorCodigo("REM-DRY-H-M");
        guardarStock(shortOferta, 6);
        guardarStock(gorraOferta, 4);
        guardarStock(remeraSinOferta, 9);

        mvc.perform(get("/ofertas"))
                .andExpect(status().isOk())
                .andExpect(view().name("publico/ofertas"))
                .andExpect(model().attributeExists("productos", "talles", "baseUrl"))
                .andExpect(content().string(containsString("Short Zero Run Hombre")))
                .andExpect(content().string(containsString("Gorra Zero Training")))
                .andExpect(content().string(not(containsString("Remera Zero Dry Fit Hombre"))))
                .andExpect(content().string(containsString("Oferta")));

        mvc.perform(get("/ofertas").param("talle", "M").param("precioMinimo", "9000")
                        .param("precioMaximo", "11000").param("orden", "precio-asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Short Zero Run Hombre")))
                .andExpect(content().string(not(containsString("Gorra Zero Training"))));
    }

    private void guardarStock(Producto producto, int cantidad) {
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setCantidadActual(cantidad);
        stock.setFecha(LocalDateTime.now());
        stock.setObservacion("Stock para prueba de catalogo");
        stockRepository.save(stock);
    }
}

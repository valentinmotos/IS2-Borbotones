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
class CatalogoPublicoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private StockRepository stockRepository;

    @Test
    void homeGeneraElMenuDesdeCategoriasActivas() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hombres")))
                .andExpect(content().string(containsString("Mujeres")))
                .andExpect(content().string(containsString("Niños")))
                .andExpect(content().string(containsString("Últimos productos agregados")));
    }

    @Test
    void categoriaYSubcategoriaSoloMuestranProductosConStockYPrecio() throws Exception {
        Producto gorra = productoService.buscarProductoPorCodigo("GOR-TRN-U");
        registrarStock(gorra, 9);
        String categoriaId = gorra.getSubCategoria().getCategoria().getId();
        String subCategoriaId = gorra.getSubCategoria().getId();

        mockMvc.perform(get("/catalogo/{categoriaId}", categoriaId))
                .andExpect(status().isOk())
                .andExpect(view().name("publico/catalogo"))
                .andExpect(content().string(containsString("Gorra Zero Training")))
                .andExpect(content().string(not(containsString("Zapatilla Zero Run"))));

        mockMvc.perform(get("/catalogo/{categoriaId}/{subCategoriaId}", categoriaId, subCategoriaId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gorra Zero Training")))
                .andExpect(content().string(containsString("Oferta")));
    }

    @Test
    void unaCategoriaInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/catalogo/no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void paginaElCatalogoYRespetaElParametroPage() throws Exception {
        Producto referencia = productoService.buscarProductoPorCodigo("REM-DRY-H-M");
        String categoriaId = referencia.getSubCategoria().getCategoria().getId();
        productoService.listarProductoActivo().stream()
                .filter(producto -> categoriaId.equals(producto.getSubCategoria().getCategoria().getId()))
                .forEach(producto -> registrarStock(producto, 5));

        mockMvc.perform(get("/catalogo/{categoriaId}", categoriaId).param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("paginaActual", 2))
                .andExpect(model().attribute("totalPaginas", 2))
                .andExpect(content().string(containsString("?page=1")));
    }

    private void registrarStock(Producto producto, int cantidad) {
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setCantidadActual(cantidad);
        stock.setFecha(LocalDateTime.now());
        stock.setObservacion("Stock para prueba de catalogo");
        stockRepository.save(stock);
    }
}

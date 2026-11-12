package com.dulcemayor.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.dulcemayor.entity.Producto;
import com.dulcemayor.repository.CategoriaRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.service.StockService;

/**
 * E3-01 de punta a punta con los productos del DataSeeder (sin token): solo productos con stock y
 * precio vigente, búsqueda, filtro por categoría, detalle y el criterio de aceptación.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogoPublicoIntegrationTest {

    private static final String URL = "/api/publico/catalogo";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ProductoRepository productos;
    @Autowired
    private CategoriaRepository categorias;
    @Autowired
    private StockService stockService;

    private Producto producto(String codigo) {
        return productos.findByCodigoIgnoreCase(codigo).orElseThrow();
    }

    @Test
    void listaSinTokenSoloLosProductosConStockYConLosDatosDelContrato() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                // «Chupetín frutal» no tiene stock en el seeder
                .andExpect(jsonPath("$[*].codigo").value(not(hasItem("CAR-001"))))
                .andExpect(jsonPath("$[?(@.codigo == 'ALF-001')].nombre").value("Alfajor de chocolate"))
                .andExpect(jsonPath("$[?(@.codigo == 'ALF-001')].precio").value(6000.0))
                .andExpect(jsonPath("$[?(@.codigo == 'ALF-001')].stock").value(50))
                .andExpect(jsonPath("$[?(@.codigo == 'ALF-001')].presentacion").value("Caja x 12"))
                .andExpect(jsonPath("$[?(@.codigo == 'ALF-001')].categoriaNombre").value("Alfajores"))
                .andExpect(jsonPath("$[?(@.codigo == 'ALF-001')].imagenId").isNotEmpty());
    }

    @Test
    void buscaPorNombreSinImportarMayusculas() throws Exception {
        mvc.perform(get(URL).param("nombre", "  ALFAJOR "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("ALF-001"));
    }

    @Test
    void filtraPorCategoria() throws Exception {
        String gomitas = categorias.findAll().stream()
                .filter(c -> c.getNombre().equals("Gomitas")).findFirst().orElseThrow().getId();

        mvc.perform(get(URL).param("categoriaId", gomitas))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("GOM-001"));
    }

    @Test
    void criterioDeAceptacionUnProductoQueLlegaAStockCeroDejaDeMostrarse() throws Exception {
        Producto chocolate = producto("CHO-001");
        int saldo = stockService.buscarStockPorProducto(chocolate.getId()).cantidadActual();
        mvc.perform(get(URL)).andExpect(jsonPath("$[*].codigo").value(hasItem("CHO-001")));

        stockService.descontar(chocolate.getId(), saldo, "Prueba E3-01");
        try {
            mvc.perform(get(URL)).andExpect(jsonPath("$[*].codigo").value(not(hasItem("CHO-001"))));
            mvc.perform(get(URL + "/" + chocolate.getId())).andExpect(status().isNotFound());
        } finally {
            stockService.reponer(chocolate.getId(), saldo, "Fin de la prueba E3-01");
        }
    }

    @Test
    void detalleDeUnProductoDisponibleY404SiNoTieneStock() throws Exception {
        mvc.perform(get(URL + "/" + producto("ALF-001").getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("ALF-001"))
                .andExpect(jsonPath("$.stock").value(50));
        mvc.perform(get(URL + "/" + producto("CAR-001").getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El producto no está disponible."));
        mvc.perform(get(URL + "/no-existe")).andExpect(status().isNotFound());
    }

    @Test
    void listaLasCategoriasSinToken() throws Exception {
        mvc.perform(get(URL + "/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre").value(hasItem("Alfajores")));
    }
}

package com.dulcemayor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.dulcemayor.entity.Producto;
import com.dulcemayor.repository.CategoriaRepository;
import com.dulcemayor.repository.ProductoRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private CategoriaRepository categorias;
    @Autowired private ProductoRepository productos;

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void reposicionDe20LlevaElSaldoDe0A20YUnDescuentoMayorSeRechaza() throws Exception {
        String id = crearProducto();

        mvc.perform(get("/api/stock/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadActual").value(0))
                .andExpect(jsonPath("$.sinStock").value(true));

        movimiento("ingresos", "{\"productoId\":\"" + id + "\",\"cantidad\":20,\"observacion\":\"Remito 1\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadActual").value(20))
                .andExpect(jsonPath("$.sinStock").value(false));

        movimiento("ajustes", "{\"productoId\":\"" + id + "\",\"diferencia\":-21}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("diferencia"));

        movimiento("ajustes", "{\"productoId\":\"" + id + "\",\"diferencia\":-5,\"observacion\":\"Rotura\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadActual").value(15));

        mvc.perform(get("/api/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productoId == '" + id + "')].cantidadActual").value(15));

        mvc.perform(get("/api/stock/" + id + "/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("AJUSTE"))
                .andExpect(jsonPath("$[0].cantidad").value(-5))
                .andExpect(jsonPath("$[1].tipo").value("INGRESO"))
                .andExpect(jsonPath("$[1].cantidad").value(20));
    }

    @Test
    @WithMockUser(roles = "JEFE")
    void validaLosDatosDeEntrada() throws Exception {
        String id = crearProducto();

        movimiento("ingresos", "{\"productoId\":\"" + id + "\",\"cantidad\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("cantidad"));
        movimiento("ajustes", "{\"productoId\":\"" + id + "\",\"diferencia\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("diferencia"));
        movimiento("ingresos", "{\"productoId\":\"inexistente\",\"cantidad\":5}")
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PREVENTOR")
    void unPreventorNoPuedeMoverStock() throws Exception {
        movimiento("ingresos", "{\"productoId\":\"cualquiera\",\"cantidad\":5}")
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/stock")).andExpect(status().isForbidden());
    }

    private ResultActions movimiento(String tipo, String json) throws Exception {
        return mvc.perform(post("/api/stock/" + tipo).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String crearProducto() {
        Producto producto = new Producto();
        producto.setCodigo("E202-" + UUID.randomUUID());
        producto.setNombre("Chupetín de prueba");
        producto.setPresentacion("Bolsa x50");
        producto.setUnidadesPorBulto(50);
        producto.setCategoria(categorias.findByEliminadoFalse().getFirst());
        return productos.save(producto).getId();
    }
}

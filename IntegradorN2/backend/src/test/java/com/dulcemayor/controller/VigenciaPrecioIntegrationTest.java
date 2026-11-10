package com.dulcemayor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.dulcemayor.entity.VigenciaPrecio;
import com.dulcemayor.repository.CategoriaRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.VigenciaPrecioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VigenciaPrecioIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private CategoriaRepository categorias;
    @Autowired private ProductoRepository productos;
    @Autowired private VigenciaPrecioRepository vigencias;

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void tresCambiosDePrecioDejanTresVigenciasSinSuperposicion() throws Exception {
        String id = crearProducto();
        String recurso = "/api/productos/" + id + "/precios";
        LocalDate hoy = LocalDate.now();

        mvc.perform(get(recurso + "/vigente")).andExpect(status().isNotFound());

        cargarPrecio(recurso, hoy, 10000).andExpect(status().isCreated());
        cargarPrecio(recurso, hoy.plusDays(10), 13000).andExpect(status().isCreated());
        cargarPrecio(recurso, hoy.plusDays(20), 15000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vigente").value(true));

        mvc.perform(get(recurso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fechaDesde").value(hoy.plusDays(20).toString()))
                .andExpect(jsonPath("$[0].fechaHasta").isEmpty())
                .andExpect(jsonPath("$[1].fechaDesde").value(hoy.plusDays(10).toString()))
                .andExpect(jsonPath("$[1].fechaHasta").value(hoy.plusDays(19).toString()))
                .andExpect(jsonPath("$[2].fechaDesde").value(hoy.toString()))
                .andExpect(jsonPath("$[2].fechaHasta").value(hoy.plusDays(9).toString()));
        mvc.perform(get(recurso + "/vigente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(15000.0));

        var guardadas = vigencias.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeDesc(id);
        assertThat(guardadas).filteredOn(v -> v.getFechaHasta() == null).hasSize(1);
        for (int i = 0; i < guardadas.size() - 1; i++) {
            VigenciaPrecio posterior = guardadas.get(i);
            VigenciaPrecio anterior = guardadas.get(i + 1);
            assertThat(anterior.getFechaHasta()).isBefore(posterior.getFechaDesde());
        }
    }

    @Test
    @WithMockUser(roles = "JEFE")
    void rechazaFechaSuperpuestaYDatosInvalidos() throws Exception {
        String id = crearProducto();
        String recurso = "/api/productos/" + id + "/precios";
        LocalDate hoy = LocalDate.now();
        cargarPrecio(recurso, hoy.plusDays(5), 8000).andExpect(status().isCreated());

        cargarPrecio(recurso, hoy.plusDays(5), 9000)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("fechaDesde"));
        cargarPrecio(recurso, hoy.minusDays(1), 9000)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("fechaDesde"));
        cargarPrecio(recurso, hoy.plusDays(6), 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("precio"));
        cargarPrecio("/api/productos/inexistente/precios", hoy, 9000).andExpect(status().isNotFound());

        assertThat(vigencias.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeDesc(id)).hasSize(1);
    }

    @Test
    @WithMockUser(roles = "PREVENTOR")
    void unPreventorNoPuedeCargarPrecios() throws Exception {
        cargarPrecio("/api/productos/cualquiera/precios", LocalDate.now(), 1000)
                .andExpect(status().isForbidden());
    }

    private ResultActions cargarPrecio(String recurso, LocalDate fechaDesde, double precio) throws Exception {
        return mvc.perform(post(recurso).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaDesde\":\"" + fechaDesde + "\",\"precio\":" + precio + "}"));
    }

    private String crearProducto() {
        Producto producto = new Producto();
        producto.setCodigo("E203-" + UUID.randomUUID());
        producto.setNombre("Alfajor de prueba");
        producto.setPresentacion("Caja");
        producto.setUnidadesPorBulto(24);
        producto.setCategoria(categorias.findByEliminadoFalse().getFirst());
        return productos.save(producto).getId();
    }
}

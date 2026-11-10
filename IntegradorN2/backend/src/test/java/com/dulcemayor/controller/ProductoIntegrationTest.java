package com.dulcemayor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.dulcemayor.repository.CategoriaRepository;
import com.dulcemayor.repository.ImagenRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private CategoriaRepository categorias;
    @Autowired private ImagenRepository imagenes;
    @Autowired private ProductoRepository productos;

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void creaEditaFiltraYDaDeBajaUnProductoConImagen() throws Exception {
        var opciones = categorias.findByEliminadoFalse();
        String categoria = opciones.get(0).getId();
        String otraCategoria = opciones.get(1).getId();
        String codigo = "E201-" + UUID.randomUUID();
        var alta = solicitud(HttpMethod.POST, "/api/productos", codigo, categoria).file(foto());
        var respuesta = mvc.perform(alta)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(codigo.toUpperCase(java.util.Locale.ROOT)))
                .andExpect(jsonPath("$.imagenId").isString())
                .andReturn().getResponse().getContentAsString();
        var creado = json.readTree(respuesta);
        String id = creado.get("id").asText();
        String imagen = creado.get("imagenId").asText();
        String recurso = "/api/productos/" + id;

        mvc.perform(get("/api/imagenes/" + imagen)).andExpect(status().isOk());
        long totalImagenes = imagenes.count();
        mvc.perform(solicitud(HttpMethod.POST, "/api/productos", " " + codigo + " ", categoria).file(foto()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("codigo"));
        assertThat(imagenes.count()).isEqualTo(totalImagenes);

        mvc.perform(get("/api/productos/buscar").param("busqueda", codigo).param("categoriaId", categoria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/productos/buscar").param("busqueda", codigo).param("categoriaId", otraCategoria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(solicitud(HttpMethod.PUT, recurso, codigo, otraCategoria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenId").value(imagen))
                .andExpect(jsonPath("$.categoriaId").value(otraCategoria));
        mvc.perform(solicitud(HttpMethod.PUT, recurso, codigo, otraCategoria).file(foto()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenId").value(org.hamcrest.Matchers.not(imagen)));

        mvc.perform(delete(recurso)).andExpect(status().isNoContent());
        mvc.perform(get(recurso)).andExpect(status().isNotFound());
        mvc.perform(get("/api/productos/buscar").param("busqueda", codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        assertThat(productos.findById(id).orElseThrow().isEliminado()).isTrue();
        mvc.perform(solicitud(HttpMethod.POST, "/api/productos", codigo, categoria).file(foto()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "JEFE")
    void rechazaAltaSinImagenConCategoriaInvalidaOArchivoInvalido() throws Exception {
        String categoria = categorias.findByEliminadoFalse().getFirst().getId();
        String codigo = "E201-VALIDACION";
        mvc.perform(solicitud(HttpMethod.POST, "/api/productos", codigo, categoria))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("imagen"));
        mvc.perform(solicitud(HttpMethod.POST, "/api/productos", codigo, "inexistente").file(foto()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("categoriaId"));
        var invalida = new MockMultipartFile("imagen", "foto.png", "image/png", new byte[] {1, 2, 3});
        mvc.perform(solicitud(HttpMethod.POST, "/api/productos", codigo, categoria).file(invalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("imagen"));
        assertThat(productos.findByCodigoIgnoreCase(codigo)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "PREVENTOR")
    void unPreventorNoPuedeAdministrarProductos() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isForbidden());
    }

    private static MockMultipartHttpServletRequestBuilder solicitud(
            HttpMethod metodo, String url, String codigo, String categoria) {
        var request = multipart(metodo, url);
        request.param("codigo", codigo).param("nombre", "Alfajor de prueba")
                .param("descripcion", "Chocolate").param("presentacion", "Caja")
                .param("unidadesPorBulto", "12").param("categoriaId", categoria);
        return request;
    }

    private static MockMultipartFile foto() throws Exception {
        return new MockMultipartFile("imagen", "producto.png", "image/png",
                new ClassPathResource("imagenes/imagen-prueba.png").getContentAsByteArray());
    }
}

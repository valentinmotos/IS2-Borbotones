package com.borbotones.integrador1;

import com.borbotones.integrador1.entities.Categoria;
import com.borbotones.integrador1.repositories.CategoriaRepository;
import com.borbotones.integrador1.services.CategoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CategoriaService categoriaService;

    @BeforeEach
    void setUp() {
        categoriaRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /categorias debe responder 200 y renderizar la vista de listado")
    void listarCategoriasRenderizaVista() throws Exception {
        categoriaService.crearCategoria("Ropa Deportiva");

        mockMvc.perform(get("/categorias"))
                .andExpect(status().isOk())
                .andExpect(view().name("categorias/listar"))
                .andExpect(model().attributeExists("categorias"))
                .andExpect(model().attribute("mostrarEliminadas", false));
    }

    @Test
    @DisplayName("GET /categorias/crear debe renderizar el formulario de alta")
    void formularioCrearRenderizaVista() throws Exception {
        mockMvc.perform(get("/categorias/crear"))
                .andExpect(status().isOk())
                .andExpect(view().name("categorias/crear"))
                .andExpect(model().attributeExists("nombre"));
    }

    @Test
    @DisplayName("POST /categorias/crear con datos válidos debe crear y redireccionar")
    void crearCategoriaValidaRedirecciona() throws Exception {
        mockMvc.perform(post("/categorias/crear")
                        .param("nombre", "Calzado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categorias"))
                .andExpect(flash().attribute("exito", "Categoría creada con éxito."));

        assertTrue(categoriaRepository.existsByNombreIgnoreCase("Calzado"));
    }

    @Test
    @DisplayName("POST /categorias/crear con nombre inválido debe mostrar error en formulario")
    void crearCategoriaInvalidaMuestraError() throws Exception {
        mockMvc.perform(post("/categorias/crear")
                        .param("nombre", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("categorias/crear"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("GET /categorias/editar/{id} debe renderizar el formulario con los datos")
    void formularioEditarRenderizaVista() throws Exception {
        categoriaService.crearCategoria("Accesorios");
        Categoria categoria = categoriaService.buscarCategoriaPorNombre("Accesorios");

        mockMvc.perform(get("/categorias/editar/" + categoria.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("categorias/editar"))
                .andExpect(model().attributeExists("categoria"));
    }

    @Test
    @DisplayName("POST /categorias/editar/{id} con datos válidos debe modificar y redireccionar")
    void editarCategoriaValidaRedirecciona() throws Exception {
        categoriaService.crearCategoria("Accesorios");
        Categoria categoria = categoriaService.buscarCategoriaPorNombre("Accesorios");

        mockMvc.perform(post("/categorias/editar/" + categoria.getId())
                        .param("nombre", "Accesorios Unisex"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categorias"))
                .andExpect(flash().attribute("exito", "Categoría modificada con éxito."));

        Categoria modificada = categoriaService.buscarCategoria(categoria.getId());
        org.junit.jupiter.api.Assertions.assertEquals("Accesorios Unisex", modificada.getNombre());
    }

    @Test
    @DisplayName("POST /categorias/eliminar/{id} debe aplicar baja lógica")
    void eliminarCategoriaAplicaBajaLogica() throws Exception {
        categoriaService.crearCategoria("Para Borrar");
        Categoria categoria = categoriaService.buscarCategoriaPorNombre("Para Borrar");

        mockMvc.perform(post("/categorias/eliminar/" + categoria.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categorias"))
                .andExpect(flash().attribute("exito", "Categoría dada de baja correctamente."));

        Categoria eliminada = categoriaRepository.findById(categoria.getId()).orElseThrow();
        assertTrue(eliminada.isEliminado());
    }

    @Test
    @DisplayName("POST /categorias/reactivar/{id} debe reactivar la categoría")
    void reactivarCategoriaRestauraEstado() throws Exception {
        categoriaService.crearCategoria("Eliminada");
        Categoria categoria = categoriaService.buscarCategoriaPorNombre("Eliminada");
        categoriaService.eliminarCategoria(categoria.getId());

        mockMvc.perform(post("/categorias/reactivar/" + categoria.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categorias?mostrarEliminadas=true"))
                .andExpect(flash().attribute("exito", "Categoría reactivada correctamente."));

        Categoria reactivada = categoriaRepository.findById(categoria.getId()).orElseThrow();
        assertFalse(reactivada.isEliminado());
    }
}

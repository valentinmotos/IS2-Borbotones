package com.zero.ecommerce;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.repositories.CategoriaRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class CategoriaAdminFlowTest {

    private static final String BASE = "/admin/categorias";

    private final MockMvc mvc;
        private final CategoriaRepository categoriaRepository;

        CategoriaAdminFlowTest(@Autowired MockMvc mvc, @Autowired CategoriaRepository categoriaRepository) {
        this.mvc = mvc;
                this.categoriaRepository = categoriaRepository;
    }

    @Test
    void adminAutenticadoPuedeListarYCrearCategoria() throws Exception {
        mvc.perform(get(BASE).with(user("admin@zero.com.ar").roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Categorías")))
                .andExpect(content().string(containsString("Nueva categoría")));

        MvcResult pagina = mvc.perform(get(BASE + "/nueva")
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andReturn();

        mvc.perform(post(BASE)
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO"))
                .with(csrf())
                .param("nombre", "Tecnología"))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "La categoría se guardó correctamente."));

        mvc.perform(get(BASE)
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tecnología")));
    }

    @Test
    void adminAutenticadoPuedeCrearSubcategoriaYRechazaDuplicado() throws Exception {
        Categoria categoria = new Categoria();
        categoria.setNombre("Ropa");
        categoriaRepository.save(categoria);

        String categoriaId = categoriaRepository.findByNombreIgnoreCaseAndEliminadoFalse("Ropa").orElseThrow().getId();

        mvc.perform(get(BASE + "/" + categoriaId + "/subcategorias/nueva")
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nueva subcategoría")));

        mvc.perform(post(BASE + "/" + categoriaId + "/subcategorias")
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO"))
                .with(csrf())
                .param("nombre", "Zapatillas"))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "La subcategoría se guardó correctamente."));

        mvc.perform(post(BASE + "/" + categoriaId + "/subcategorias")
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO"))
                .with(csrf())
                .param("nombre", "Zapatillas"))
                .andExpect(redirectedUrl(BASE + "/" + categoriaId + "/subcategorias/nueva"))
                .andExpect(flash().attribute("error",
                        "Ya existe una subcategoría con ese nombre dentro de la categoría seleccionada."));
    }
}

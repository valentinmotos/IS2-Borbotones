package com.borbotones.integrador1;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Integrador1ApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void inicioRenderizaLaVistaThymeleaf() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("inicio"))
                .andExpect(content().string(containsString("TP Integrador 1")));
    }

    @Test
    void paginasDelTemplateRenderizanSusVistas() throws Exception {
        Map<String, String> paginas = Map.of(
                "/productos", "productos",
                "/producto-detalle", "producto-detalle",
                "/carrito", "carrito",
                "/contacto", "contacto",
                "/demo/inicio-2", "inicio-02",
                "/demo/inicio-3", "inicio-03");

        for (Map.Entry<String, String> pagina : paginas.entrySet()) {
            mockMvc.perform(get(pagina.getKey()))
                    .andExpect(status().isOk())
                    .andExpect(view().name(pagina.getValue()));
        }
    }

    @Test
    void recursosEstaticosDelTemplateEstanDisponibles() throws Exception {
        mockMvc.perform(get("/cozastore/css/main.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));

        mockMvc.perform(get("/cozastore/vendor/bootstrap/css/bootstrap.min.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(containsString("Bootstrap  v5.3.8")));

        mockMvc.perform(get("/cozastore/vendor/bootstrap/js/bootstrap.bundle.min.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bootstrap v5.3.8")));
    }

    @Test
    void usuariosRenderizaElListadoConElTemplate() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/lista"))
                .andExpect(content().string(containsString("Crear usuario")))
                .andExpect(content().string(containsString("/cozastore/css/main.css")));
    }

    @Test
    void usuariosRenderizaElFormularioDeAltaConElTemplate() throws Exception {
        mockMvc.perform(get("/usuarios/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/formulario"))
                .andExpect(content().string(containsString("Nombre de usuario")))
                .andExpect(content().string(containsString("/cozastore/vendor/bootstrap/css/bootstrap.min.css")));
    }

    @Test
    void errorEnAltaMantieneElFormularioEnModoCreacion() throws Exception {
        mockMvc.perform(post("/usuarios/crear")
                        .param("id", "")
                        .param("nombreUsuario", "usuario-prueba-error")
                        .param("clave", "123")
                        .param("rol", "CLIENTE"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/formulario"))
                .andExpect(content().string(containsString("Crear usuario")))
                .andExpect(content().string(containsString("action=\"/usuarios/crear\"")))
                .andExpect(content().string(containsString("id=\"clave\"")));
    }

    @Test
    void buscadorFiltraUsuariosPorNombre() throws Exception {
        mockMvc.perform(post("/usuarios/crear")
                        .param("nombreUsuario", "usuario-encontrable")
                        .param("clave", "secreto")
                        .param("rol", "CLIENTE"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/usuarios/crear")
                        .param("nombreUsuario", "usuario-oculto")
                        .param("clave", "secreto")
                        .param("rol", "CLIENTE"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/usuarios").param("buscar", "enCONTRABLE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("usuario-encontrable")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("usuario-oculto"))));
    }
}

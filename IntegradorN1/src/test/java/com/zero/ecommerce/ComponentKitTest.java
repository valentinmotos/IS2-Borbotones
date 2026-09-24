package com.zero.ecommerce;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
class ComponentKitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRenderDevComponentsPage() throws Exception {
        mockMvc.perform(get("/dev/componentes"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Kit de componentes")))
            .andExpect(content().string(containsString("mensajes")))
            .andExpect(content().string(containsString("tabla")))
            .andExpect(content().string(containsString("formulario")))
            .andExpect(content().string(containsString("No hay registros para mostrar.")))
            .andExpect(content().string(containsString("Abrir modal")))
            .andExpect(content().string(containsString("Remera Zero Pro")))
            .andExpect(content().string(containsString("Pendiente de pago")))
            .andExpect(content().string(containsString("Ventas del mes")));
    }
}

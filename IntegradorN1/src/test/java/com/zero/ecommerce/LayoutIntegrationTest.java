package com.zero.ecommerce;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
class LayoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicLayoutAndExamplePageShouldRender() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("publico/inicio"));

        mockMvc.perform(get("/dev/ejemplo-publico"))
            .andExpect(status().isOk())
            .andExpect(view().name("dev/ejemplo-publico"));
    }

    @Test
    void adminExamplePageShouldRender() throws Exception {
        mockMvc.perform(get("/dev/ejemplo-admin"))
            .andExpect(status().isOk())
            .andExpect(view().name("dev/ejemplo-admin"));
    }
}

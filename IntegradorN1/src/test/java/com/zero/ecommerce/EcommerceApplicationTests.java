package com.zero.ecommerce;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;

import com.zero.ecommerce.exception.ErrorServiceException;

// Base en memoria: los tests no tocan data/zero.db. halt_on_error hace fallar el test si
// Hibernate no puede crear alguna tabla (con ddl-auto=update solo lo loguearía).
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Import(EcommerceApplicationTests.ControllerQueFalla.class)
class EcommerceApplicationTests {

    private final MockMvc mockMvc;

    EcommerceApplicationTests(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void contextLoads() {
    }

    @Test
    void inicioResponde() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("publico/inicio"))
                .andExpect(content().string(containsString("Zero – proyecto base")));
    }

    @Test
    void errorServiceVuelveALaPaginaAnteriorConMensaje() throws Exception {
        mockMvc.perform(get("/test/falla").header("Referer", "http://localhost:8080/admin/lista?pagina=2"))
                .andExpect(redirectedUrl("/admin/lista?pagina=2"))
                .andExpect(flash().attribute("error", "Mensaje de prueba"));
    }

    @Test
    void errorServiceSinRefererVuelveAlInicio() throws Exception {
        mockMvc.perform(get("/test/falla"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Mensaje de prueba"));
    }

    @Controller
    static class ControllerQueFalla {

        @GetMapping("/test/falla")
        String falla() throws ErrorServiceException {
            throw new ErrorServiceException("Mensaje de prueba");
        }
    }
}

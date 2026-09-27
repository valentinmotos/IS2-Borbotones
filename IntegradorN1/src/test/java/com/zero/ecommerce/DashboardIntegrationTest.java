package com.zero.ecommerce;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class DashboardIntegrationTest {

    private final MockMvc mvc;

    DashboardIntegrationTest(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void muestraDashboardCompletoYEnlazaLasVentasAlMesActual() throws Exception {
        LocalDate hoy = LocalDate.now();
        String inicioMes = hoy.withDayOfMonth(1).toString();
        String finMes = hoy.withDayOfMonth(hoy.lengthOfMonth()).toString();

        mvc.perform(get("/admin").with(user("admin@zero.com.ar").roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ventas del mes")))
                .andExpect(content().string(containsString("vs. mes anterior")))
                .andExpect(content().string(containsString("Ventas de los últimos 6 meses")))
                .andExpect(content().string(containsString("Top 5 productos")))
                .andExpect(content().string(containsString(
                        "/admin/pedidos?desde=" + inicioMes + "&amp;hasta=" + finMes)))
                .andExpect(content().string(not(containsString("/admin/usuarios?rol=CLIENTE"))));
    }

    @Test
    void enlazaClientesFiltradosCuandoElUsuarioEsJefe() throws Exception {
        mvc.perform(get("/admin").with(user("jefe@zero.com.ar").roles("JEFE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/usuarios?rol=CLIENTE")));
    }

    @Test
    void protegeDashboardDeUsuariosCliente() throws Exception {
        mvc.perform(get("/admin").with(user("cliente@zero.com.ar").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }
}

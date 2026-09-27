package com.zero.ecommerce;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.ReporteStockDTO;
import com.zero.ecommerce.services.ReporteStockService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class ReporteStockIntegrationTest {

    private final MockMvc mvc;
    private final ReporteStockService reporteStockService;

    ReporteStockIntegrationTest(@Autowired MockMvc mvc, @Autowired ReporteStockService reporteStockService) {
        this.mvc = mvc;
        this.reporteStockService = reporteStockService;
    }

    @Test
    void muestraReporteTotalesBarraEstadosYFiltraPorTarjeta() throws Exception {
        ReporteStockDTO reporte = reporteStockService.generar();
        int totalEstados = reporte.cantidadBueno() + reporte.cantidadRegular() + reporte.cantidadMalo();

        mvc.perform(get("/admin/reportes/stock")
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("reporte", reporte))
                .andExpect(content().string(containsString("Reporte de productos y stock")))
                .andExpect(content().string(containsString(reporte.sucursal())))
                .andExpect(content().string(containsString("Stock bueno")))
                .andExpect(content().string(containsString("Stock regular")))
                .andExpect(content().string(containsString("Stock malo")))
                .andExpect(content().string(containsString("role=\"progressbar\"")));
        org.assertj.core.api.Assertions.assertThat(totalEstados).isEqualTo(reporte.cantidadProductos());

        mvc.perform(get("/admin/reportes/stock").param("estado", "MALO")
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("estado", "MALO"))
                .andExpect(content().string(containsString("CAL-FIT-M")))
                .andExpect(content().string(containsString("15 %")))
                .andExpect(content().string(not(containsString("COL-YOG-U"))));
    }

    @Test
    void protegeElReporteDeUsuariosCliente() throws Exception {
        mvc.perform(get("/admin/reportes/stock")
                .with(user("cliente@zero.com.ar").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }
}

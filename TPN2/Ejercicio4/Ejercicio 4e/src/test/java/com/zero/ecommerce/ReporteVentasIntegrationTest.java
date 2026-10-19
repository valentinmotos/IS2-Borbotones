package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.DetalleVentaDTO;
import com.zero.ecommerce.dto.ReporteVentasDTO;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ReporteVentasService;

/**
 * Pruebas de integración para E5-01 · Reporte de ventas.
 * Verifica la lógica de negocio en ReporteVentasService y el controlador /admin/reportes/ventas.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class ReporteVentasIntegrationTest {

    private static final String BASE = "/admin/reportes/ventas";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReporteVentasService reporteService;

    private MockHttpSession sesion;

    @BeforeEach
    void iniciarSesion() throws Exception {
        sesion = login("admin@zero.com.ar", "Admin123!");
    }

    private MockHttpSession login(String correo, String clave) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesionNueva = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(sesionNueva).param(token.getParameterName(), token.getToken())
                .param("username", correo).param("password", clave))
                .andExpect(redirectedUrl("/admin"));
        return sesionNueva;
    }

    @Test
    void generarReporte_fechaDesdePosteriorAHasta_lanzaExcepcion() {
        LocalDate desde = LocalDate.of(2026, 3, 31);
        LocalDate hasta = LocalDate.of(2026, 3, 1);

        assertThatThrownBy(() -> reporteService.generar(desde, hasta))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("no puede ser posterior");
    }

    @Test
    void generarReporte_marzo_coincideTotalConSumaDeDetalle() throws ErrorServiceException {
        int anio = LocalDate.now().getYear();
        LocalDate desde = LocalDate.of(anio, 3, 1);
        LocalDate hasta = LocalDate.of(anio, 3, 31);

        ReporteVentasDTO reporte = reporteService.generar(desde, hasta);

        assertThat(reporte).isNotNull();
        // El seeder agrega estas tres ventas fijas de marzo. Si la prueba corre durante marzo también pueden
        // corresponder ventas relativas a la fecha actual, que igualmente deben incluirse por estar pagadas.
        assertThat(reporte.cantidadCompras()).isGreaterThanOrEqualTo(3);
        assertThat(reporte.unidadesVendidas()).isGreaterThanOrEqualTo(12);
        assertThat(reporte.detalle()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(reporte.detalle()).extracting(DetalleVentaDTO::identificadorCompra)
                .contains("Venta N.º 9", "Venta N.º 10", "Venta N.º 11")
                .doesNotContain("ORD-DEMO0001", "ORD-DEMO0005", "ORD-DEMO0007");
        assertThat(reporte.detalle().stream().map(DetalleVentaDTO::idFactura).distinct().count())
                .isEqualTo(reporte.cantidadCompras());

        // Criterio de aceptación: el total coincide con la suma del detalle
        double sumaDetalle = reporte.detalle().stream().mapToDouble(DetalleVentaDTO::subtotal).sum();
        assertThat(reporte.montoTotal()).isEqualTo(sumaDetalle);

        int sumaUnidades = reporte.detalle().stream().mapToInt(DetalleVentaDTO::cantidad).sum();
        assertThat(reporte.unidadesVendidas()).isEqualTo(sumaUnidades);

        // Subtotales por forma de pago no vacíos
        assertThat(reporte.subtotalesPorFormaPago()).isNotEmpty();
        assertThat(reporte.subtotalesPorFormaPago()).hasSize(3);
        assertThat(reporte.subtotalesPorFormaPago().stream()
                .mapToDouble(subtotal -> subtotal.montoTotal()).sum()).isEqualTo(reporte.montoTotal());
    }

    @Test
    void vistaVentas_usuarioAdmin_muestraPaginaYContenido() throws Exception {
        int anio = LocalDate.now().getYear();

        mvc.perform(get(BASE)
                        .param("desde", anio + "-03-01")
                        .param("hasta", anio + "-03-31")
                        .session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reporte de ventas")))
                .andExpect(content().string(containsString("Unidades vendidas")))
                .andExpect(content().string(containsString("Subtotales por forma de pago")));
    }

    @Test
    void vistaVentas_rangoInvalidoConservaFiltroYMuestraErrorSinEstadoVacio() throws Exception {
        mvc.perform(get(BASE)
                        .param("desde", "2026-03-31")
                        .param("hasta", "2026-03-01")
                        .session(sesion))
                .andExpect(status().isOk())
                .andExpect(model().attribute("reporte", org.hamcrest.Matchers.nullValue()))
                .andExpect(content().string(containsString("no puede ser posterior")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("No hay ventas pagadas en el período seleccionado"))));
    }

    @Test
    void vistaVentas_sinSesion_redirigeALogin() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }
}

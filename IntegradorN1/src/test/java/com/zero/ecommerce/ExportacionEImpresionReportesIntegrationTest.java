package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

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

import com.zero.ecommerce.utils.ExportadorCsv;

/**
 * Pruebas de integración para E5-02 · Exportación e impresión de reportes.
 * Verifica el exportador CSV con BOM UTF-8 y los endpoints/vistas de los tres reportes.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class ExportacionEImpresionReportesIntegrationTest {

    @Autowired
    private MockMvc mvc;

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
                .andExpect(status().is3xxRedirection());
        return sesionNueva;
    }

    @Test
    void exportadorCsv_agregaBomUtf8YEscapaCampos() {
        List<String> encabezados = List.of("Producto", "Precio", "Descripción");
        List<List<String>> filas = List.of(
                List.of("Remera \"Dry Fit\"", "1500.50", "Remera deportiva, talle M\nCon tilde en Canción")
        );

        byte[] resultado = ExportadorCsv.exportar(encabezados, filas);

        assertThat(resultado).isNotNull();
        // Verificar BOM UTF-8 (EF BB BF)
        assertThat(resultado[0]).isEqualTo((byte) 0xEF);
        assertThat(resultado[1]).isEqualTo((byte) 0xBB);
        assertThat(resultado[2]).isEqualTo((byte) 0xBF);

        String contenido = new String(resultado, StandardCharsets.UTF_8);
        assertThat(contenido).contains("Producto,Precio,Descripción");
        assertThat(contenido).contains("\"Remera \"\"Dry Fit\"\"\"");
        assertThat(contenido).contains("Canción");
    }

    @Test
    void exportarReporteVentas_devuelveCsvUtf8ConHeaders() throws Exception {
        int anio = LocalDate.now().getYear();

        MvcResult result = mvc.perform(get("/admin/reportes/ventas/exportar")
                        .param("desde", anio + "-03-01")
                        .param("hasta", anio + "-03-31")
                        .session(sesion))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"reporte_ventas_")))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);

        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).contains("Fecha de compra,Producto,Categoría,Cantidad,Precio unitario,Subtotal,ID Compra,Forma de pago");
    }

    @Test
    void exportarReporteStock_devuelveCsvUtf8ConHeaders() throws Exception {
        MvcResult result = mvc.perform(get("/admin/reportes/stock/exportar").session(sesion))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("reporte_stock.csv")))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);

        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).contains("Código,Producto,Talle,Categoría,Stock actual,Stock de referencia,Estado stock");
    }

    @Test
    void exportarReporteProveedores_devuelveCsvUtf8ConHeaders() throws Exception {
        MvcResult result = mvc.perform(get("/admin/reportes/proveedores/exportar").session(sesion))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("reporte_proveedores.csv")))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);

        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).contains("Código,Producto,Talle,Categoría,Proveedor recomendado,Precio de costo más bajo,Fecha de compra");
    }
}

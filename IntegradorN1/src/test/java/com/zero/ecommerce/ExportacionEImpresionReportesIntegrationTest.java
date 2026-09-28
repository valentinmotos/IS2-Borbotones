package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
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

import com.zero.ecommerce.entities.enums.EstadoStock;
import com.zero.ecommerce.services.EmpresaService;
import com.zero.ecommerce.services.ReporteProveedoresService;
import com.zero.ecommerce.services.ReporteStockService;
import com.zero.ecommerce.services.ReporteVentasService;
import com.zero.ecommerce.utils.ExportadorCsv;

/** E5-02: exportación a CSV y encabezado de impresión de los tres reportes. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class ExportacionEImpresionReportesIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReporteVentasService reporteVentasService;

    @Autowired
    private ReporteStockService reporteStockService;

    @Autowired
    private ReporteProveedoresService reporteProveedoresService;

    @Autowired
    private EmpresaService empresaService;

    private MockHttpSession sesion;

    @BeforeEach
    void iniciarSesion() throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        sesion = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(sesion).param(token.getParameterName(), token.getToken())
                .param("username", "admin@zero.com.ar").param("password", "Admin123!"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void exportadorAgregaElBomSeparaConPuntoYComaYEscapaLosCampos() {
        byte[] resultado = ExportadorCsv.exportar(List.of("Producto", "Precio", "Descripción"),
                List.of(Arrays.asList("Remera \"Dry Fit\"", ExportadorCsv.monto(1500.5), "Talle M; con tilde\nCanción"),
                        Arrays.asList("Short", null, "")));

        assertThat(Arrays.copyOf(resultado, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        assertThat(new String(resultado, 3, resultado.length - 3, StandardCharsets.UTF_8)).isEqualTo(
                "Producto;Precio;Descripción\r\n"
                        + "\"Remera \"\"Dry Fit\"\"\";1500,50;\"Talle M; con tilde\nCanción\"\r\n"
                        + "Short;;\r\n");
    }

    @Test
    void elCsvDeVentasTraeLasMismasLineasQueElReporteDelPeriodo() throws Exception {
        int anio = LocalDate.now().getYear();
        LocalDate desde = LocalDate.of(anio, 3, 1);
        LocalDate hasta = LocalDate.of(anio, 3, 31);

        MvcResult resultado = mvc.perform(get("/admin/reportes/ventas/exportar").session(sesion)
                        .param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition",
                        containsString("reporte_ventas_" + desde + "_a_" + hasta + ".csv")))
                .andReturn();

        List<String> lineas = lineas(resultado);
        int esperadas = reporteVentasService.generar(desde, hasta).detalle().size();
        assertThat(esperadas).isPositive();
        assertThat(lineas.get(0))
                .isEqualTo("Fecha de compra;Producto;Categoría;Cantidad;Precio unitario;Subtotal;ID Compra;Forma de pago");
        assertThat(lineas).hasSize(esperadas + 1);
        assertThat(lineas.subList(1, lineas.size())).allSatisfy(linea -> assertThat(linea).contains("/03/" + anio));
    }

    @Test
    void elCsvDeStockRespetaElFiltroDeEstado() throws Exception {
        MvcResult resultado = mvc.perform(get("/admin/reportes/stock/exportar").session(sesion).param("estado", "MALO"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("reporte_stock_")))
                .andReturn();

        List<String> lineas = lineas(resultado);
        long malos = reporteStockService.generar().productos().stream()
                .filter(producto -> producto.estado() == EstadoStock.MALO).count();
        assertThat(lineas.get(0)).isEqualTo(
                "Código;Producto;Talle;Categoría;Subcategoría;Stock actual;Stock de referencia;Porcentaje;Estado");
        assertThat(lineas).hasSize((int) malos + 1);
        assertThat(lineas.subList(1, lineas.size())).allSatisfy(linea -> assertThat(linea).endsWith(";Malo"));
    }

    @Test
    void elCsvDeProveedoresTraeUnaLineaPorProducto() throws Exception {
        MvcResult resultado = mvc.perform(get("/admin/reportes/proveedores/exportar").session(sesion))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("reporte_proveedores_")))
                .andReturn();

        List<String> lineas = lineas(resultado);
        assertThat(lineas.get(0)).isEqualTo("Código;Producto;Talle;Categoría;Subcategoría;Proveedor recomendado;"
                + "Precio de costo más bajo;Fecha de compra");
        assertThat(lineas).hasSize(reporteProveedoresService.generar("", "").productos().size() + 1);
    }

    @Test
    void losTresReportesTienenElEncabezadoDeImpresionConLaEmpresa() throws Exception {
        String razonSocial = empresaService.buscarSedeCentral().getRazonSocial();
        int anio = LocalDate.now().getYear();
        for (String reporte : List.of("ventas", "stock", "proveedores")) {
            mvc.perform(get("/admin/reportes/" + reporte).session(sesion)
                            .param("desde", anio + "-03-01").param("hasta", anio + "-03-31"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("zero-print-header")))
                    .andExpect(content().string(containsString(razonSocial)))
                    .andExpect(content().string(containsString("/admin/reportes/" + reporte + "/exportar")))
                    .andExpect(content().string(containsString("window.print()")));
        }
    }

    private List<String> lineas(MvcResult resultado) {
        byte[] bytes = resultado.getResponse().getContentAsByteArray();
        assertThat(Arrays.copyOf(bytes, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        return List.of(new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8).split("\r\n"));
    }
}

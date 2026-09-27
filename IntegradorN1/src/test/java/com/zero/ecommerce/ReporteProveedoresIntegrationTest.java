package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.zero.ecommerce.dto.DetalleFacturaItemDTO;
import com.zero.ecommerce.dto.PrecioProveedorDTO;
import com.zero.ecommerce.dto.ProductoProveedorDTO;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.FacturaProveedorService;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.ProveedorService;
import com.zero.ecommerce.services.ReporteProveedoresService;

/** Reporte de proveedores (E5-05) con las compras recibidas del seeder y compras de prueba a 3 proveedores. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class ReporteProveedoresIntegrationTest {

    private static final String BASE = "/admin/reportes/proveedores";
    // Zapatilla Run 41: solo está en la compra pedida N.º 4 del seeder, así que no tiene precios recibidos.
    private static final String ZAPATILLA = "ZAP-RUN-41";
    private final MockMvc mvc;
    private final ReporteProveedoresService reporteService;
    private final FacturaProveedorService facturaProveedorService;
    private final ProveedorService proveedorService;
    private final FormaDePagoService formaDePagoService;
    private final ProductoService productoService;
    private MockHttpSession sesion;

    ReporteProveedoresIntegrationTest(@Autowired MockMvc mvc, @Autowired ReporteProveedoresService reporteService,
            @Autowired FacturaProveedorService facturaProveedorService, @Autowired ProveedorService proveedorService,
            @Autowired FormaDePagoService formaDePagoService, @Autowired ProductoService productoService) {
        this.mvc = mvc;
        this.reporteService = reporteService;
        this.facturaProveedorService = facturaProveedorService;
        this.proveedorService = proveedorService;
        this.formaDePagoService = formaDePagoService;
        this.productoService = productoService;
    }

    // Los reportes los usan JEFE y ADMINISTRATIVO: se prueba con el administrativo.
    @BeforeEach
    void iniciarSesion() throws Exception {
        sesion = login("admin@zero.com.ar", "Admin123!");
    }

    @Test
    void elReporteMuestraLasComprasRecibidasDelSeederYNoLasPedidas() throws Exception {
        List<ProductoProveedorDTO> filas = reporteService.generar().productos();
        assertThat(filas).extracting(ProductoProveedorDTO::codigo).contains("CAL-FIT-S", "MOC-URB-U")
                .doesNotContain(ZAPATILLA, "REM-DRY-H-M");

        mvc.perform(get(BASE).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Reporte de proveedores")))
                .andExpect(content().string(containsString("CAL-FIT-S")))
                .andExpect(content().string(containsString("Indumentaria Atlética San Juan")))
                .andExpect(content().string(containsString("$6.500,00")))
                .andExpect(content().string(not(containsString(ZAPATILLA))));
    }

    @Test
    void conComprasATresProveedoresRecomiendaElDeMenorCostoYMuestraLaDiferenciaDeLosOtros() throws Exception {
        Producto zapatilla = productoService.buscarProductoPorCodigo(ZAPATILLA);
        Proveedor cuyo = proveedor("Distribuidora Deportiva Cuyo S.A.");
        recibirCompra(proveedor("Calzados y Textiles del Plata S.R.L."), zapatilla, 32000);
        recibirCompra(cuyo, zapatilla, 30000);
        recibirCompra(proveedor("Accesorios Fitness Andina"), zapatilla, 36000);

        ProductoProveedorDTO fila = reporteService.generar(ZAPATILLA, null).productos().get(0);
        assertThat(fila.recomendado().razonSocial()).isEqualTo("Distribuidora Deportiva Cuyo S.A.");
        assertThat(fila.proveedores()).extracting(PrecioProveedorDTO::precioCosto).containsExactly(30000.0, 32000.0,
                36000.0);
        assertThat(reporteService.buscarProveedorMasEconomico(zapatilla.getId())).contains(cuyo);

        mvc.perform(get(BASE).param("buscar", "zap-run-41").session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("$30.000,00")))
                .andExpect(content().string(containsString("Comparar 3 proveedores")))
                .andExpect(content().string(containsString("+6,7 %")))
                .andExpect(content().string(containsString("+20,0 %")))
                .andExpect(content().string(not(containsString("CAL-FIT-S"))));
    }

    @Test
    void filtraPorCategoria() throws Exception {
        // Mochila Urban es de Accesorios y Calza Fit S de Mujeres; las dos tienen compras recibidas en el seeder.
        String accesorios = productoService.buscarProductoPorCodigo("MOC-URB-U").getSubCategoria().getCategoria()
                .getId();

        mvc.perform(get(BASE).param("categoria", accesorios).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("MOC-URB-U")))
                .andExpect(content().string(not(containsString("CAL-FIT-S"))));
    }

    @Test
    void sinComprasRecibidasNoHayProveedorMasEconomico() throws Exception {
        assertThat(reporteService.buscarProveedorMasEconomico(productoService.buscarProductoPorCodigo(ZAPATILLA)
                .getId())).isEmpty();
    }

    private void recibirCompra(Proveedor proveedor, Producto producto, double precioCosto)
            throws ErrorServiceException {
        String formaDePago = formaDePagoService.listarFormaDePagoActivo().get(0).getId();
        FacturaProveedor compra = facturaProveedorService.crearFactura(proveedor.getId(), formaDePago,
                List.of(new DetalleFacturaItemDTO(producto.getId(), 5, precioCosto)));
        facturaProveedorService.recibirFactura(compra.getId());
    }

    private Proveedor proveedor(String razonSocial) {
        return proveedorService.listarProveedorActivo().stream()
                .filter(p -> p.getRazonSocial().equals(razonSocial))
                .findFirst()
                .orElseThrow();
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
}

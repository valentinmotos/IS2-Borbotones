package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.services.FacturaProveedorService;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.ProveedorService;
import com.zero.ecommerce.services.StockService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class CompraProveedorIntegrationTest {

    private static final String BASE = "/admin/compras";
    private final MockMvc mvc;
    private final FacturaProveedorService facturaProveedorService;
    private final ProveedorService proveedorService;
    private final FormaDePagoService formaDePagoService;
    private final ProductoService productoService;
    private final StockService stockService;
    private MockHttpSession sesion;

    CompraProveedorIntegrationTest(@Autowired MockMvc mvc, @Autowired FacturaProveedorService facturaProveedorService,
            @Autowired ProveedorService proveedorService, @Autowired FormaDePagoService formaDePagoService,
            @Autowired ProductoService productoService, @Autowired StockService stockService) {
        this.mvc = mvc;
        this.facturaProveedorService = facturaProveedorService;
        this.proveedorService = proveedorService;
        this.formaDePagoService = formaDePagoService;
        this.productoService = productoService;
        this.stockService = stockService;
    }

    // Las compras las usan JEFE y ADMINISTRATIVO: se prueba con el administrativo.
    @BeforeEach
    void iniciarSesion() throws Exception {
        sesion = login("admin@zero.com.ar", "Admin123!");
    }

    @Test
    void elSeederCargaDosComprasPedidasYElListadoLasMuestra() throws Exception {
        List<FacturaProveedor> compras = facturaProveedorService.listarFacturaActivo();
        assertThat(compras).hasSize(2).allMatch(c -> c.getEstado() == EstadoFactura.SIN_DEFINIR);
        assertThat(compras).extracting(FacturaProveedor::getNumeroFactura).containsExactly(2L, 1L);

        mvc.perform(get(BASE).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Distribuidora Deportiva Cuyo S.A.")))
                .andExpect(content().string(containsString("Calzados y Textiles del Plata S.R.L.")))
                .andExpect(content().string(containsString("Sin definir")))
                .andExpect(content().string(containsString("2 compras")));
    }

    @Test
    void elListadoFiltraPorProveedorEstadoYFechas() throws Exception {
        String cuyo = idProveedor("Distribuidora Deportiva Cuyo S.A.");
        mvc.perform(get(BASE).param("proveedor", cuyo).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Distribuidora Deportiva Cuyo S.A.</td>")))
                .andExpect(content().string(not(containsString("Calzados y Textiles del Plata S.R.L.</td>"))))
                .andExpect(content().string(containsString("1 compra")));
        mvc.perform(get(BASE).param("estado", "PAGADA").session(sesion))
                .andExpect(content().string(containsString("0 compras")));
        mvc.perform(get(BASE).param("desde", "2000-01-01").param("hasta", "2000-12-31").session(sesion))
                .andExpect(content().string(containsString("0 compras")));
        mvc.perform(get(BASE).param("desde", "2026-05-02").param("hasta", "2026-05-01").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La fecha desde no puede ser posterior a la fecha hasta.")));
    }

    @Test
    void creaUnaCompraDeTresProductosQueQuedaPedidaConElTotalDeLosSubtotales() throws Exception {
        String remeraM = idProducto("REM-DRY-H-M");
        String remeraL = idProducto("REM-DRY-H-L");
        String calza = idProducto("CAL-FIT-S");

        mvc.perform(get(BASE + "/nueva").session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"plantilla-detalle\"")))
                .andExpect(content().string(containsString("name=\"detalles[INDICE].productoId\"")))
                .andExpect(content().string(containsString("name=\"detalles[0].cantidad\"")))
                .andExpect(content().string(containsString("REM-DRY-H-M · Remera Zero Dry Fit Hombre (talle M)")))
                .andExpect(content().string(containsString("/vendor/template/vendor/select2/select2.min.js")))
                .andExpect(content().string(containsString("/js/compra-detalles.js")));

        MvcResult resultado = mvc.perform(postConCsrf(BASE, BASE + "/nueva")
                .param("proveedorId", idProveedor("Distribuidora Deportiva Cuyo S.A."))
                .param("formaDePagoId", formaDePagoService.listarFormaDePagoActivo().get(0).getId())
                .param("detalles[0].productoId", remeraM).param("detalles[0].cantidad", "20")
                .param("detalles[0].precioUnitario", "4500")
                .param("detalles[1].productoId", remeraL).param("detalles[1].cantidad", "10")
                .param("detalles[1].precioUnitario", "4550.50")
                // Un renglón quitado del medio deja un hueco en los índices.
                .param("detalles[3].productoId", calza).param("detalles[3].cantidad", "5")
                .param("detalles[3].precioUnitario", "3000"))
                .andExpect(flash().attribute("exito",
                        "Compra N.º 3 creada: quedó pedida. Podés avisarle al proveedor por WhatsApp."))
                .andReturn();

        FacturaProveedor compra = facturaProveedorService.listarFacturaActivo().get(0);
        assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo(BASE + "/" + compra.getId());
        assertThat(compra.getNumeroFactura()).isEqualTo(3);
        assertThat(compra.getEstado()).isEqualTo(EstadoFactura.SIN_DEFINIR);
        assertThat(compra.getDetalles()).hasSize(3);
        double sumaSubtotales = compra.getDetalles().stream().mapToDouble(DetalleFactura::getSubtotal).sum();
        assertThat(sumaSubtotales).isEqualTo(90000 + 45505 + 15000);
        assertThat(compra.getTotalPagado()).isEqualTo(sumaSubtotales);
        // Una compra pedida todavía no mueve el stock.
        assertThat(stockService.buscarStockActual(remeraM)).isZero();

        mvc.perform(get(BASE + "/" + compra.getId()).session(sesion).flashAttrs(resultado.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Compra N.º 3 creada")))
                .andExpect(content().string(containsString("$150.505,00")))
                .andExpect(content().string(containsString("$4.550,50")))
                .andExpect(content().string(containsString("href=\"https://wa.me/5492614123456?text=Hola")))
                .andExpect(content().string(containsString("Avisar al proveedor por WhatsApp")));
    }

    @Test
    void unaCompraConProductosRepetidosSeRechazaYVuelveConLosDatos() throws Exception {
        String remeraM = idProducto("REM-DRY-H-M");
        MvcResult resultado = mvc.perform(postConCsrf(BASE, BASE + "/nueva")
                .param("proveedorId", idProveedor("Distribuidora Deportiva Cuyo S.A."))
                .param("formaDePagoId", formaDePagoService.listarFormaDePagoActivo().get(0).getId())
                .param("detalles[0].productoId", remeraM).param("detalles[0].cantidad", "20")
                .param("detalles[0].precioUnitario", "4500")
                .param("detalles[1].productoId", remeraM).param("detalles[1].cantidad", "7")
                .param("detalles[1].precioUnitario", "4500"))
                .andExpect(redirectedUrl(BASE + "/nueva"))
                .andExpect(flash().attribute("error", "El producto Remera Zero Dry Fit Hombre (talle M) está repetido: "
                        + "cargalo en un solo renglón con la cantidad total."))
                .andReturn();
        assertThat(facturaProveedorService.listarFacturaActivo()).hasSize(2);

        mvc.perform(get(BASE + "/nueva").session(sesion).flashAttrs(resultado.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"detalles[1].cantidad\"")))
                .andExpect(content().string(containsString("value=\"7\"")));
    }

    @Test
    void unaCantidadQueNoEsNumeroMuestraUnErrorEnLugarDeFallar() throws Exception {
        mvc.perform(postConCsrf(BASE, BASE + "/nueva")
                .param("proveedorId", idProveedor("Distribuidora Deportiva Cuyo S.A."))
                .param("detalles[0].productoId", idProducto("REM-DRY-H-M")).param("detalles[0].cantidad", "diez")
                .param("detalles[0].precioUnitario", "4500"))
                .andExpect(redirectedUrl(BASE + "/nueva"))
                .andExpect(flash().attribute("error",
                        "Revisá las cantidades y los precios de costo: tienen que ser números."));
    }

    @Test
    void unaCompraInexistenteDevuelve404() throws Exception {
        mvc.perform(get(BASE + "/no-existe").session(sesion)).andExpect(status().isNotFound());
    }

    private String idProveedor(String razonSocial) {
        return proveedorService.listarProveedorActivo(razonSocial).get(0).getId();
    }

    private String idProducto(String codigo) throws Exception {
        return productoService.buscarProductoPorCodigo(codigo).getId();
    }

    // Login real contra el formulario, con las cuentas que crea el seeder.
    private MockHttpSession login(String correo, String clave) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesionNueva = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(sesionNueva).param(token.getParameterName(), token.getToken())
                .param("username", correo).param("password", clave))
                .andExpect(redirectedUrl("/admin"));
        return sesionNueva;
    }

    // Obtiene el token del formulario renderizado: prueba también que la página se renderiza sin errores.
    private MockHttpServletRequestBuilder postConCsrf(String destino, String formulario) throws Exception {
        MvcResult pagina = mvc.perform(get(formulario).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        return post(destino).session(sesion).param(token.getParameterName(), token.getToken());
    }
}

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

import com.zero.ecommerce.dto.MovimientoStockDTO;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.services.FacturaProveedorService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.StockService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class RecepcionMercaderiaIntegrationTest {

    private static final String COMPRAS = "/admin/compras";
    private static final String PRODUCTOS = "/admin/productos";
    private final MockMvc mvc;
    private final FacturaProveedorService facturaProveedorService;
    private final ProductoService productoService;
    private final StockService stockService;
    private MockHttpSession sesion;

    RecepcionMercaderiaIntegrationTest(@Autowired MockMvc mvc,
            @Autowired FacturaProveedorService facturaProveedorService, @Autowired ProductoService productoService,
            @Autowired StockService stockService) {
        this.mvc = mvc;
        this.facturaProveedorService = facturaProveedorService;
        this.productoService = productoService;
        this.stockService = stockService;
    }

    @BeforeEach
    void iniciarSesion() throws Exception {
        sesion = login("admin@zero.com.ar", "Admin123!");
    }

    @Test
    void recibirUnaCompraDe20UnidadesLlevaElStockDe0A20YNoSePuedeRecibirDeNuevo() throws Exception {
        // La compra pedida N.º 3 del seeder trae 20 Remeras Dry Fit M, que todavía no tienen stock.
        FacturaProveedor compra = compraNumero(3);
        String remeraM = productoService.buscarProductoPorCodigo("REM-DRY-H-M").getId();
        assertThat(stockService.buscarStockActual(remeraM)).isZero();

        mvc.perform(get(COMPRAS + "/" + compra.getId()).session(sesion))
                .andExpect(content().string(containsString("Marcar como recibida")))
                .andExpect(content().string(containsString("Anular compra")));
        mvc.perform(postConCsrf(COMPRAS + "/" + compra.getId() + "/recibir", COMPRAS + "/" + compra.getId()))
                .andExpect(redirectedUrl(COMPRAS + "/" + compra.getId()))
                .andExpect(flash().attribute("exito", "Compra N.º 3 recibida: se sumó la mercadería al stock."));

        assertThat(facturaProveedorService.buscarFactura(compra.getId()).getEstado()).isEqualTo(EstadoFactura.PAGADA);
        assertThat(stockService.buscarStockActual(remeraM)).isEqualTo(20);
        assertThat(stockService.buscarStockActual(productoService.buscarProductoPorCodigo("REM-DRY-H-L").getId()))
                .isEqualTo(15);
        List<MovimientoStockDTO> historial = stockService.listarHistorial(remeraM);
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).cantidad()).isEqualTo(20);
        assertThat(historial.get(0).saldo()).isEqualTo(20);
        assertThat(historial.get(0).comprobante()).isEqualTo("Compra N.º 3");

        // Ya recibida: el detalle no ofrece las acciones y un segundo intento se rechaza sin tocar el stock.
        mvc.perform(get(COMPRAS + "/" + compra.getId()).session(sesion))
                .andExpect(content().string(not(containsString("Marcar como recibida"))));
        mvc.perform(postConCsrf(COMPRAS + "/" + compra.getId() + "/recibir", COMPRAS))
                .andExpect(redirectedUrl(COMPRAS + "/" + compra.getId()))
                .andExpect(flash().attribute("error", "La compra N.º 3 ya fue recibida: no se puede recibir de nuevo."));
        assertThat(stockService.buscarStockActual(remeraM)).isEqualTo(20);
    }

    @Test
    void elDetalleDelProductoMuestraElStockActualYElHistorialDeMovimientos() throws Exception {
        // Calza Fit M: recibió 20 en la compra N.º 1 y se vendieron 17 en la venta de demostración N.º 1.
        String calzaM = productoService.buscarProductoPorCodigo("CAL-FIT-M").getId();
        String compra1 = compraNumero(1).getId();

        mvc.perform(get(PRODUCTOS).session(sesion))
                .andExpect(content().string(containsString("href=\"" + PRODUCTOS + "/" + calzaM + "\"")));
        mvc.perform(get(PRODUCTOS + "/" + calzaM).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Historial de movimientos")))
                .andExpect(content().string(containsString("3 unidades")))
                .andExpect(content().string(containsString("href=\"/admin/compras/" + compra1 + "\"")))
                .andExpect(content().string(containsString(">+20</td>")))
                .andExpect(content().string(containsString(">-17</td>")))
                .andExpect(content().string(containsString("Venta N.º 1")));
        mvc.perform(get(PRODUCTOS + "/no-existe").session(sesion)).andExpect(status().isNotFound());
    }

    @Test
    void anularUnaCompraPedidaLaDejaAnuladaSinMoverElStock() throws Exception {
        FacturaProveedor compra = compraNumero(4);
        String zapatilla = productoService.buscarProductoPorCodigo("ZAP-RUN-42").getId();

        mvc.perform(postConCsrf(COMPRAS + "/" + compra.getId() + "/anular", COMPRAS + "/" + compra.getId()))
                .andExpect(redirectedUrl(COMPRAS + "/" + compra.getId()))
                .andExpect(flash().attribute("exito", "Compra N.º 4 anulada."));

        assertThat(facturaProveedorService.buscarFactura(compra.getId()).getEstado()).isEqualTo(EstadoFactura.ANULADA);
        assertThat(stockService.buscarStockActual(zapatilla)).isZero();
        mvc.perform(postConCsrf(COMPRAS + "/" + compra.getId() + "/recibir", COMPRAS))
                .andExpect(flash().attribute("error", "La compra N.º 4 está anulada: no se puede recibir."));
        assertThat(stockService.buscarStockActual(zapatilla)).isZero();
    }

    @Test
    void elSeederDejaProductosEnLosTresNivelesDelReporteDeStock() throws Exception {
        // Porcentaje = stock actual ÷ saldo después de la última recepción (decisiones de diseño, RF29).
        assertThat(stock("COL-YOG-U")).isEqualTo(12);   // 12/12: Bueno
        assertThat(stock("CAL-FIT-S")).isEqualTo(7);    // 7/20 = 35 %: Regular
        assertThat(stock("TOP-MOV-M")).isEqualTo(6);    // 6/15 = 40 %: Regular
        assertThat(stock("CAL-FIT-M")).isEqualTo(3);    // 3/20 = 15 %: Malo
        assertThat(stock("BOL-GYM-U")).isZero();        // 0/8: Malo
        assertThat(stock("ZAP-RUN-42")).isZero();       // nunca se recibió: queda fuera del reporte
    }

    private int stock(String codigo) throws Exception {
        return stockService.buscarStockActual(productoService.buscarProductoPorCodigo(codigo).getId());
    }

    private FacturaProveedor compraNumero(long numero) {
        return facturaProveedorService.listarFacturaActivo().stream()
                .filter(c -> c.getNumeroFactura() == numero)
                .findFirst()
                .orElseThrow();
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

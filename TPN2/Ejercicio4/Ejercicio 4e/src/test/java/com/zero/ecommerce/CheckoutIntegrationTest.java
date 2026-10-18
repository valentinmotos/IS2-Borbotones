package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.services.CarritoService;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.OrdenCompraService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.VentaService;
import com.zero.ecommerce.services.VigenciaPrecioService;

/**
 * Checkout del cliente (E4-02) con los datos del seeder: Lucía tiene el carrito abierto ORD-DEMO0006 con una gorra
 * GOR-TRN-U. Las ventas del seeder llegan hasta la factura N.º 11.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class CheckoutIntegrationTest {

    private static final String BASE = "/cliente/checkout";
    private final MockMvc mvc;
    private final OrdenCompraService ordenCompraService;
    private final CarritoService carritoService;
    private final FormaDePagoService formaDePagoService;
    private final ProductoService productoService;
    private final VigenciaPrecioService vigenciaPrecioService;

    CheckoutIntegrationTest(@Autowired MockMvc mvc, @Autowired OrdenCompraService ordenCompraService,
            @Autowired CarritoService carritoService, @Autowired FormaDePagoService formaDePagoService,
            @Autowired ProductoService productoService, @Autowired VigenciaPrecioService vigenciaPrecioService) {
        this.mvc = mvc;
        this.ordenCompraService = ordenCompraService;
        this.carritoService = carritoService;
        this.formaDePagoService = formaDePagoService;
        this.productoService = productoService;
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    @Test
    void elCheckoutMuestraElResumenLaDireccionYLasFormasDePagoActivas() throws Exception {
        MockHttpSession lucia = login("lucia.gomez@mail.com");

        mvc.perform(get(BASE).session(lucia)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Resumen del pedido")))
                .andExpect(content().string(containsString("Gorra")))
                .andExpect(content().string(containsString("San Martín 1250, Ciudad de Mendoza")))
                .andExpect(content().string(containsString("href=\"/cliente/perfil\"")))
                .andExpect(content().string(containsString("Transferencia")))
                .andExpect(content().string(containsString("Mercado Pago (Billetera Virtual)")))
                .andExpect(content().string(containsString("Confirmar compra")));
    }

    @Test
    void confirmarConTransferenciaDejaLaOrdenPendienteDePagoLaFacturaConLosPreciosYElCarritoVacio()
            throws Exception {
        MockHttpSession lucia = login("lucia.gomez@mail.com");
        String idCarrito = idCarritoDeLucia();
        double precioGorra = vigenciaPrecioService
                .buscarPrecioVigente(productoService.buscarProductoPorCodigo("GOR-TRN-U").getId());

        mvc.perform(postConCsrf(lucia).param("idFormaPago", idFormaDePago(TipoPago.TRANSFERENCIA)))
                .andExpect(redirectedUrl(BASE + "/registrada/" + idCarrito));

        OrdenCompra orden = ordenCompraService.buscarPedido(idCarrito);
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_PAGO);
        FacturaCliente factura = ordenCompraService.buscarFacturaDePedido(idCarrito).orElseThrow();
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.SIN_DEFINIR);
        assertThat(factura.getNumeroFactura()).isEqualTo(12);
        assertThat(factura.getCliente()).isSameAs(orden.getCliente());
        assertThat(factura.getFormaDePago().getTipoPago()).isEqualTo(TipoPago.TRANSFERENCIA);
        assertThat(factura.getDetalles()).singleElement().satisfies((DetalleFactura d) -> {
            assertThat(d.getProducto().getCodigo()).isEqualTo("GOR-TRN-U");
            assertThat(d.getCantidad()).isEqualTo(1);
            assertThat(d.getPrecioUnitario()).isEqualTo(precioGorra);
        });
        assertThat(factura.getTotalPagado()).isEqualTo(orden.getTotal());
        assertThat(carritoService.contarItemsCarrito(orden.getCliente().getId())).isZero();

        mvc.perform(get(BASE + "/registrada/" + idCarrito).session(lucia)).andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-DEMO0006")))
                .andExpect(content().string(containsString("transferencia por el total")))
                .andExpect(content().string(containsString("N.º 12")));
    }

    @Test
    void confirmarConMercadoPagoSimuladoMuestraCompraRegistrada() throws Exception {
        MockHttpSession lucia = login("lucia.gomez@mail.com");
        String idCarrito = idCarritoDeLucia();

        mvc.perform(postConCsrf(lucia).param("idFormaPago", idFormaDePago(TipoPago.BILLETERA_VIRTUAL)))
                .andExpect(redirectedUrl(BASE + "/registrada/" + idCarrito));
        mvc.perform(get(BASE + "/registrada/" + idCarrito).session(lucia))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mercado Pago simulado")));
    }

    @Test
    void sinFormaDePagoVuelveAlCheckoutConElError() throws Exception {
        MockHttpSession lucia = login("lucia.gomez@mail.com");

        mvc.perform(postConCsrf(lucia))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("error", "Elegí una forma de pago."));
    }

    @Test
    void unClienteSinPerfilCompletoVaAlPerfil() throws Exception {
        MockHttpSession cliente = login("cliente@zero.com.ar");

        mvc.perform(get(BASE).session(cliente))
                .andExpect(redirectedUrl("/cliente/perfil"))
                .andExpect(flash().attribute("error", VentaService.MENSAJE_PERFIL_INCOMPLETO));
    }

    @Test
    void conElCarritoVacioVuelveAlCarrito() throws Exception {
        MockHttpSession martin = login("martin.perez@mail.com");

        mvc.perform(get(BASE).session(martin))
                .andExpect(redirectedUrl("/cliente/carrito"))
                .andExpect(flash().attribute("error", "Tu carrito está vacío."));
    }

    @Test
    void noSePuedeVerLaCompraRegistradaDeOtroCliente() throws Exception {
        MockHttpSession lucia = login("lucia.gomez@mail.com");
        String deMartin = ordenCompraService.listarPedidoActivo().stream()
                .filter(o -> "ORD-DEMO0007".equals(o.getIdentificadorCompra()))
                .map(OrdenCompra::getId).findFirst().orElseThrow();

        mvc.perform(get(BASE + "/registrada/" + deMartin).session(lucia)).andExpect(status().isForbidden());
    }

    private String idCarritoDeLucia() throws Exception {
        String idLucia = ordenCompraService.listarPedidoActivo().stream()
                .filter(o -> "ORD-DEMO0001".equals(o.getIdentificadorCompra()))
                .map(o -> o.getCliente().getId()).findFirst().orElseThrow();
        return carritoService.obtenerCarrito(idLucia).getId();
    }

    private String idFormaDePago(TipoPago tipo) {
        return formaDePagoService.listarFormaDePagoActivo().stream()
                .filter(f -> f.getTipoPago() == tipo)
                .findFirst().orElseThrow().getId();
    }

    // Login real contra el formulario, con las cuentas que crea el seeder.
    private MockHttpSession login(String correo) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesion = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(sesion).param(token.getParameterName(), token.getToken())
                .param("username", correo).param("password", "Cliente123!"))
                .andExpect(status().is3xxRedirection());
        return sesion;
    }

    // Obtiene el token de la página del checkout renderizada en la sesión.
    private MockHttpServletRequestBuilder postConCsrf(MockHttpSession sesion) throws Exception {
        MvcResult resultado = mvc.perform(get(BASE).session(sesion)).andExpect(status().isOk()).andReturn();
        CsrfToken token = (CsrfToken) resultado.getRequest().getAttribute(CsrfToken.class.getName());
        return post(BASE).session(sesion).param(token.getParameterName(), token.getToken());
    }
}

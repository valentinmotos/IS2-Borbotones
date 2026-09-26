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

import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.services.OrdenCompraService;

/**
 * "Mis compras" del cliente (E4-04) con los pedidos de demostración del seeder. Lucía tiene ORD-DEMO0001 (pendiente
 * de pago, transferencia), ORD-DEMO0003 (pendiente de entrega) y el carrito ORD-DEMO0006; el resto son de Martín.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class CompraClienteIntegrationTest {

    private static final String BASE = "/cliente/compras";
    private final MockMvc mvc;
    private final OrdenCompraService ordenCompraService;
    private MockHttpSession sesion;

    CompraClienteIntegrationTest(@Autowired MockMvc mvc, @Autowired OrdenCompraService ordenCompraService) {
        this.mvc = mvc;
        this.ordenCompraService = ordenCompraService;
    }

    @BeforeEach
    void iniciarSesion() throws Exception {
        sesion = login("lucia.gomez@mail.com", "Cliente123!");
    }

    @Test
    void elClienteVeSusComprasSinElCarritoNiLasDeOtroCliente() throws Exception {
        mvc.perform(get(BASE).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-DEMO0001")))
                .andExpect(content().string(containsString("ORD-DEMO0003")))
                .andExpect(content().string(containsString("Transferencia")))
                .andExpect(content().string(containsString("Pendiente de pago")))
                .andExpect(content().string(not(containsString("ORD-DEMO0006"))))
                .andExpect(content().string(not(containsString("ORD-DEMO0002"))));
    }

    @Test
    void elDetalleMuestraLaLineaDeTiempoYLosDatosDeLaCompra() throws Exception {
        mvc.perform(get(BASE + "/" + idPedido("ORD-DEMO0003")).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("zero-timeline")))
                .andExpect(content().string(containsString("aria-current=\"step\"")))
                .andExpect(content().string(containsString("N.º 5")))
                .andExpect(content().string(containsString("San Martín 1250, Ciudad de Mendoza (5500), Mendoza")))
                .andExpect(content().string(containsString("Remera")))
                // Ya se pagó: no se puede anular ni pagar.
                .andExpect(content().string(not(containsString("Anular compra"))))
                .andExpect(content().string(not(containsString("Pagar ahora"))));
    }

    @Test
    void unaCompraSinPagarPorTransferenciaSePuedeAnularPeroNoPagarConMercadoPago() throws Exception {
        mvc.perform(get(BASE + "/" + idPedido("ORD-DEMO0001")).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Anular compra")))
                .andExpect(content().string(not(containsString("Pagar ahora"))));
    }

    @Test
    void unaCompraSinPagarConMercadoPagoTieneElBotonPagarAhora() throws Exception {
        MockHttpSession martin = login("martin.perez@mail.com", "Cliente123!");
        String id = idPedido("ORD-DEMO0007");

        mvc.perform(get(BASE + "/" + id).session(martin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Pagar ahora")))
                .andExpect(content().string(containsString("href=\"/cliente/pago/" + id + "\"")));
    }

    @Test
    void anularUnaCompraSinPagarLaDejaAnuladaConSuFactura() throws Exception {
        String id = idPedido("ORD-DEMO0001");

        mvc.perform(postConCsrf(BASE + "/" + id + "/anular", BASE + "/" + id))
                .andExpect(redirectedUrl(BASE + "/" + id))
                .andExpect(flash().attribute("exito", "Anulaste la compra ORD-DEMO0001."));

        assertThat(ordenCompraService.buscarPedido(id).getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ANULADA);
        assertThat(ordenCompraService.buscarFacturaDePedido(id).orElseThrow().getEstado())
                .isEqualTo(EstadoFactura.ANULADA);
    }

    @Test
    void noSePuedeAnularUnaCompraYaEnviada() throws Exception {
        String id = idPedido("ORD-DEMO0003");

        mvc.perform(postConCsrf(BASE + "/" + id + "/anular", BASE + "/" + id))
                .andExpect(redirectedUrl(BASE + "/" + id))
                .andExpect(flash().attribute("error", "No se puede anular una orden que ya fue enviada."));
        assertThat(ordenCompraService.buscarPedido(id).getEstadoOrdenCompra())
                .isEqualTo(EstadoOrdenCompra.PENDIENTE_ENTREGA);
    }

    @Test
    void noPuedeVerNiAnularLaCompraDeOtroCliente() throws Exception {
        String deMartin = idPedido("ORD-DEMO0007");

        mvc.perform(get(BASE + "/" + deMartin).session(sesion)).andExpect(status().isForbidden());
        mvc.perform(postConCsrf(BASE + "/" + deMartin + "/anular", BASE))
                .andExpect(status().isForbidden());
        assertThat(ordenCompraService.buscarPedido(deMartin).getEstadoOrdenCompra())
                .isEqualTo(EstadoOrdenCompra.PENDIENTE_PAGO);
    }

    @Test
    void unaCompraInexistenteDevuelve404() throws Exception {
        mvc.perform(get(BASE + "/no-existe").session(sesion)).andExpect(status().isNotFound());
    }

    private String idPedido(String identificador) {
        return ordenCompraService.listarPedidoActivo().stream()
                .filter(o -> identificador.equals(o.getIdentificadorCompra()))
                .map(OrdenCompra::getId)
                .findFirst().orElseThrow();
    }

    // Login real contra el formulario, con las cuentas que crea el seeder.
    private MockHttpSession login(String correo, String clave) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesionNueva = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(sesionNueva).param(token.getParameterName(), token.getToken())
                .param("username", correo).param("password", clave))
                .andExpect(status().is3xxRedirection());
        return sesionNueva;
    }

    // Obtiene el token de una página renderizada de la sesión.
    private MockHttpServletRequestBuilder postConCsrf(String destino, String pagina) throws Exception {
        MvcResult resultado = mvc.perform(get(pagina).session(sesion)).andExpect(status().isOk()).andReturn();
        CsrfToken token = (CsrfToken) resultado.getRequest().getAttribute(CsrfToken.class.getName());
        return post(destino).session(sesion).param(token.getParameterName(), token.getToken());
    }
}

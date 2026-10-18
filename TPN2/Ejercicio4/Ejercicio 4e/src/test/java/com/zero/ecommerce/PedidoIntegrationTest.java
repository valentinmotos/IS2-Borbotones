package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.repositories.OrdenCompraRepository;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.NotificacionCompraService;
import com.zero.ecommerce.services.OrdenCompraService;

/** Panel de pedidos (E4-06) con los pedidos de demostración del seeder: uno por estado y un carrito abierto. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class PedidoIntegrationTest {

    private static final String BASE = "/admin/pedidos";
    private final MockMvc mvc;
    private final OrdenCompraService ordenCompraService;
    private final OrdenCompraRepository ordenCompraRepository;
    private final FormaDePagoService formaDePagoService;
    @MockitoBean
    private NotificacionCompraService notificacionCompraService;
    private MockHttpSession sesion;

    PedidoIntegrationTest(@Autowired MockMvc mvc, @Autowired OrdenCompraService ordenCompraService,
            @Autowired OrdenCompraRepository ordenCompraRepository, @Autowired FormaDePagoService formaDePagoService) {
        this.mvc = mvc;
        this.ordenCompraService = ordenCompraService;
        this.ordenCompraRepository = ordenCompraRepository;
        this.formaDePagoService = formaDePagoService;
    }

    // El panel lo usan JEFE y ADMINISTRATIVO: se prueba con el administrativo.
    @BeforeEach
    void iniciarSesion() throws Exception {
        sesion = login("admin@zero.com.ar", "Admin123!");
    }

    @Test
    void elListadoMuestraLosPedidosSinLosCarritosAbiertos() throws Exception {
        assertThat(ordenCompraService.listarPedidoActivo()).extracting(OrdenCompra::getIdentificadorCompra)
                .contains("ORD-DEMO0001", "ORD-DEMO0002", "ORD-DEMO0003", "ORD-DEMO0004", "ORD-DEMO0005")
                .doesNotContain("ORD-DEMO0006");

        mvc.perform(get(BASE).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-DEMO0001")))
                .andExpect(content().string(containsString("Lucía Gómez")))
                .andExpect(content().string(containsString("Mercado Pago")))
                .andExpect(content().string(containsString("Pendiente de entrega")))
                .andExpect(content().string(not(containsString("ORD-DEMO0006"))));
    }

    @Test
    void lasTarjetasCuentanLosPedidosPorEstadoYFiltranAlTocarlas() throws Exception {
        assertThat(ordenCompraService.contarPedidoPorEstado()).containsEntry("PENDIENTE_PAGO", 2L)
                .containsEntry("PENDIENTE_ENVIO", 1L).containsEntry("PENDIENTE_ENTREGA", 1L)
                .containsEntry("ENTREGADO", 1L).containsEntry("ANULADA", 1L);

        mvc.perform(get(BASE).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/admin/pedidos?estado=PENDIENTE_ENVIO\"")));
        mvc.perform(get(BASE).param("estado", "PENDIENTE_ENVIO").session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-DEMO0002")))
                .andExpect(content().string(not(containsString("ORD-DEMO0001"))))
                .andExpect(content().string(containsString("aria-current=\"true\"")));
    }

    @Test
    void seEncuentraUnPedidoPorClienteYEstado() throws Exception {
        mvc.perform(get(BASE).param("cliente", "martin.perez").param("estado", "ENTREGADO").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-DEMO0004")))
                .andExpect(content().string(not(containsString("ORD-DEMO0002"))))
                .andExpect(content().string(containsString("1 pedido")));
    }

    @Test
    void unRangoDeFechasInvertidoMuestraElError() throws Exception {
        mvc.perform(get(BASE).param("desde", "2026-09-30").param("hasta", "2026-09-01").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La fecha desde no puede ser posterior a la fecha hasta.")));
    }

    @Test
    void elDetalleMuestraLoNecesarioParaPrepararElEnvio() throws Exception {
        mvc.perform(get(BASE + "/" + idPedido("ORD-DEMO0003")).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("zero-timeline")))
                .andExpect(content().string(containsString("Pago realizado")))
                .andExpect(content().string(containsString("261 412-3456")))
                .andExpect(content().string(containsString("San Martín 1250, Ciudad de Mendoza (5500), Mendoza")))
                .andExpect(content().string(containsString("lucia.gomez@mail.com")))
                .andExpect(content().string(containsString("REM-DRY-H-L")))
                .andExpect(content().string(containsString("Efectivo")))
                .andExpect(content().string(containsString("N.º 5")))
                .andExpect(content().string(containsString("Administrativo Zero")));
    }

    @Test
    void elDetalleDeUnPedidoAnuladoNoMuestraLaLineaDeTiempo() throws Exception {
        mvc.perform(get(BASE + "/" + idPedido("ORD-DEMO0005")).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("El pedido fue anulado")))
                .andExpect(content().string(not(containsString("zero-timeline-paso"))));
    }

    @Test
    void unCarritoAbiertoOInexistenteDevuelve404() throws Exception {
        String carrito = ordenCompraRepository.findByEliminadoFalseOrderByFechaDesc().stream()
                .filter(o -> o.getEstadoOrdenCompra() == EstadoOrdenCompra.PENDIENTE_COMPLETAR)
                .findFirst().orElseThrow().getId();

        mvc.perform(get(BASE + "/" + carrito).session(sesion)).andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/no-existe").session(sesion)).andExpect(status().isNotFound());
    }

    @Test
    void unClienteNoPuedeEntrarAlPanelDePedidos() throws Exception {
        mvc.perform(get(BASE).with(user("cliente@zero.com.ar").roles("CLIENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void unPedidoEnEfectivoRecorreTodoElFlujoDesdeElPanel() throws Exception {
        String id = idPedido("ORD-DEMO0001");
        FacturaCliente factura = ordenCompraService.buscarFacturaDePedido(id).orElseThrow();
        factura.setFormaDePago(formaDePagoService.listarFormaDePagoActivo().stream()
                .filter(forma -> forma.getTipoPago() == TipoPago.EFECTIVO).findFirst().orElseThrow());

        mvc.perform(get(BASE + "/" + id).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmar pago")))
                .andExpect(content().string(not(containsString("Marcar como enviado"))));

        mvc.perform(post(BASE + "/" + id + "/confirmar-pago").with(csrf()).session(sesion))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "/" + id));
        assertThat(ordenCompraService.buscarPedido(id).getEstadoOrdenCompra())
                .isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.PAGADA);
        assertThat(factura.getEmpleado()).isNotNull();
        assertThat(factura.getEmpleado().getUsuario().getNombreUsuario()).isEqualTo("admin@zero.com.ar");

        mvc.perform(get(BASE + "/" + id).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Marcar como enviado")));
        mvc.perform(post(BASE + "/" + id + "/marcar-enviado").with(csrf()).session(sesion))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "/" + id));
        assertThat(ordenCompraService.buscarPedido(id).getEstadoOrdenCompra())
                .isEqualTo(EstadoOrdenCompra.PENDIENTE_ENTREGA);

        mvc.perform(get(BASE + "/" + id).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Marcar como entregado")));
        mvc.perform(post(BASE + "/" + id + "/marcar-entregado").with(csrf()).session(sesion))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "/" + id));
        assertThat(ordenCompraService.buscarPedido(id).getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ENTREGADO);
        verify(notificacionCompraService, times(3)).notificarCambioEstado(org.mockito.ArgumentMatchers.any());

        mvc.perform(get(BASE + "/" + id).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Entregado")))
                .andExpect(content().string(not(containsString("Marcar como entregado"))));
    }

    @Test
    void mercadoPagoSimuladoPermiteConfirmacionManual() throws Exception {
        String id = idPedido("ORD-DEMO0007");
        mvc.perform(get(BASE + "/" + id).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmar pago")));

        mvc.perform(post(BASE + "/" + id + "/confirmar-pago").with(csrf()).session(sesion))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "/" + id));
        assertThat(ordenCompraService.buscarPedido(id).getEstadoOrdenCompra())
                .isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
        assertThat(ordenCompraService.buscarFacturaDePedido(id).orElseThrow().getEstado())
                .isEqualTo(EstadoFactura.PAGADA);
    }

    @Test
    void anularExigeMotivoYRegistraAlEmpleado() throws Exception {
        String id = idPedido("ORD-DEMO0002");

        mvc.perform(post(BASE + "/" + id + "/anular").with(csrf()).session(sesion).param("motivo", " "))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "/" + id));
        assertThat(ordenCompraService.buscarPedido(id).getEstadoOrdenCompra())
                .isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);

        mvc.perform(post(BASE + "/" + id + "/anular").with(csrf()).session(sesion)
                .param("motivo", "El cliente solicitó cancelar la compra."))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "/" + id));
        assertThat(ordenCompraService.buscarPedido(id).getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ANULADA);
        FacturaCliente factura = ordenCompraService.buscarFacturaDePedido(id).orElseThrow();
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.ANULADA);
        assertThat(factura.getEmpleado().getUsuario().getNombreUsuario()).isEqualTo("admin@zero.com.ar");
    }

    private String idPedido(String identificador) {
        return ordenCompraService.listarPedidoActivo().stream()
                .filter(o -> identificador.equals(o.getIdentificadorCompra()))
                .findFirst().orElseThrow().getId();
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
}

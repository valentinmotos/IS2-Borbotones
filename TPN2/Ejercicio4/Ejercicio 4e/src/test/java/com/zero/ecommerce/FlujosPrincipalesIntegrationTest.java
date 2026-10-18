package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.zero.ecommerce.dto.DetalleFacturaItemDTO;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CarritoService;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.EmailService;
import com.zero.ecommerce.services.FacturaProveedorService;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.OrdenCompraService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.ProveedorService;
import com.zero.ecommerce.services.StockService;
import com.zero.ecommerce.services.UsuarioService;

/**
 * Tests de integración de los flujos principales (E5-08), los que se muestran en la exposición. Corren con el perfil
 * {@code test}: una base SQLite en un archivo aparte que se borra antes de empezar (así el seeder parte de una base
 * vacía) y al terminar. El {@link EmailService} es mock para no mandar correos reales. No es
 * {@code @Transactional}: cada request corre en su propia transacción, igual que en producción.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FlujosPrincipalesIntegrationTest {

    private static final Path BASE_DE_TEST = Path.of("target", "zero-test.db");
    private static final String CLAVE_CLIENTES = "Cliente123!";
    private static final String LUCIA = "lucia.gomez@mail.com";
    private static final String MARTIN = "martin.perez@mail.com";
    private static final String GORRA = "GOR-TRN-U";
    private static final String REMERA = "REM-DRY-H-M";

    private final MockMvc mvc;
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final CarritoService carritoService;
    private final OrdenCompraService ordenCompraService;
    private final FormaDePagoService formaDePagoService;
    private final ProductoService productoService;
    private final ProveedorService proveedorService;
    private final FacturaProveedorService facturaProveedorService;
    private final StockService stockService;
    @MockitoBean
    private EmailService emailService;

    FlujosPrincipalesIntegrationTest(@Autowired MockMvc mvc, @Autowired UsuarioService usuarioService,
            @Autowired ClienteService clienteService, @Autowired CarritoService carritoService,
            @Autowired OrdenCompraService ordenCompraService,
            @Autowired FormaDePagoService formaDePagoService, @Autowired ProductoService productoService,
            @Autowired ProveedorService proveedorService, @Autowired FacturaProveedorService facturaProveedorService,
            @Autowired StockService stockService) {
        this.mvc = mvc;
        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
        this.carritoService = carritoService;
        this.ordenCompraService = ordenCompraService;
        this.formaDePagoService = formaDePagoService;
        this.productoService = productoService;
        this.proveedorService = proveedorService;
        this.facturaProveedorService = facturaProveedorService;
        this.stockService = stockService;
    }

    // Corre antes de levantar el contexto. El archivo se borra al salir de la JVM, cuando ya se cerró la conexión.
    @BeforeAll
    static void empezarDesdeUnaBaseVacia() throws IOException {
        Files.deleteIfExists(BASE_DE_TEST);
        BASE_DE_TEST.toFile().deleteOnExit();
    }

    @Test
    void registroActivacionYLogin() throws Exception {
        String correo = "nueva.clienta@test.com";
        String clave = "Zapatilla42";

        mvc.perform(post("/registro").with(csrf()).param("correo", correo)
                        .param("clave", clave).param("confirmacion", clave))
                .andExpect(redirectedUrl("/registro/activar?correo=nueva.clienta%40test.com"));

        // El código sale del correo de activación, que queda capturado en el mock.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(emailService).enviar(eq(correo), anyString(), eq("activacion"), variables.capture());
        String codigo = (String) variables.getValue().get("codigo");
        assertThat(codigo).matches("\\d{6}");

        mvc.perform(login(correo, clave)).andExpect(redirectedUrl("/login?error=activacion"));

        mvc.perform(post("/registro/activar").with(csrf()).param("correo", correo).param("codigo", codigo))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("exito", "¡Tu cuenta quedó activada! Ya podés ingresar."));
        assertThat(usuarioService.buscarUsuarioPorNombre(correo).getCodigoActivacion()).isNull();

        mvc.perform(login(correo, clave)).andExpect(redirectedUrl("/"));
    }

    @Test
    void carritoConfirmacionPagoDescuentoDeStockYFacturaPagada() throws Exception {
        // Lucía tiene en el carrito del seeder 1 gorra; suma 2 remeras y elige Mercado Pago simulado.
        MockHttpSession lucia = sesion(LUCIA, CLAVE_CLIENTES);
        String idOrden = carritoService.obtenerCarrito(idCliente(LUCIA)).getId();
        int gorrasAntes = stock(GORRA);
        int remerasAntes = stock(REMERA);

        mvc.perform(post("/cliente/carrito/agregar").session(lucia).with(csrf())
                        .param("idProducto", idProducto(REMERA)).param("cantidad", "2"))
                .andExpect(redirectedUrl("/cliente/carrito"));
        mvc.perform(post("/cliente/checkout").session(lucia).with(csrf())
                        .param("idFormaPago", idFormaDePago(TipoPago.BILLETERA_VIRTUAL)))
                .andExpect(redirectedUrl("/cliente/checkout/registrada/" + idOrden));

        // Confirmar no mueve el stock: baja recién con el pago.
        assertThat(estadoOrden(idOrden)).isEqualTo(EstadoOrdenCompra.PENDIENTE_PAGO);
        assertThat(estadoFactura(idOrden)).isEqualTo(EstadoFactura.SIN_DEFINIR);
        assertThat(stock(REMERA)).isEqualTo(remerasAntes);

        // El administrador confirma el pago simulado; una repetición no vuelve a descontar stock.
        MockHttpSession admin = sesion("admin@zero.com.ar", "Admin123!");
        mvc.perform(post("/admin/pedidos/" + idOrden + "/confirmar-pago").session(admin).with(csrf()))
                .andExpect(flash().attributeExists("exito"));
        mvc.perform(post("/admin/pedidos/" + idOrden + "/confirmar-pago").session(admin).with(csrf()))
                .andExpect(flash().attributeExists("error"));

        assertThat(estadoOrden(idOrden)).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
        assertThat(estadoFactura(idOrden)).isEqualTo(EstadoFactura.PAGADA);
        assertThat(stock(GORRA)).isEqualTo(gorrasAntes - 1);
        assertThat(stock(REMERA)).isEqualTo(remerasAntes - 2);
    }

    @Test
    void compraAProveedorRecepcionYAumentoDeStock() throws Exception {
        MockHttpSession admin = sesion("admin@zero.com.ar", "Admin123!");
        int remerasAntes = stock(REMERA);
        FacturaProveedor compra = facturaProveedorService.crearFactura(
                proveedorService.listarProveedorActivo().get(0).getId(), idFormaDePago(TipoPago.TRANSFERENCIA),
                List.of(new DetalleFacturaItemDTO(idProducto(REMERA), 10, 8000.0)));

        // Mientras está pedida no suma stock.
        assertThat(stock(REMERA)).isEqualTo(remerasAntes);

        String detalle = "/admin/compras/" + compra.getId();
        mvc.perform(post(detalle + "/recibir").session(admin).with(csrf()))
                .andExpect(redirectedUrl(detalle))
                .andExpect(flash().attributeExists("exito"));
        assertThat(facturaProveedorService.buscarFactura(compra.getId()).getEstado()).isEqualTo(EstadoFactura.PAGADA);
        assertThat(stock(REMERA)).isEqualTo(remerasAntes + 10);

        // Recibirla de nuevo no vuelve a sumar.
        mvc.perform(post(detalle + "/recibir").session(admin).with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertThat(stock(REMERA)).isEqualTo(remerasAntes + 10);
    }

    @Test
    void anulacionPorElClienteAntesDelPagoYPorElAdminDespuesDelPagoConReingresoDeStock() throws Exception {
        MockHttpSession martin = sesion(MARTIN, CLAVE_CLIENTES);
        MockHttpSession admin = sesion("admin@zero.com.ar", "Admin123!");
        int gorrasAntes = stock(GORRA);

        // El cliente anula una compra que todavía no pagó: no hay stock que devolver.
        String sinPagar = comprarGorraConTransferencia(martin);
        mvc.perform(post("/cliente/compras/" + sinPagar + "/anular").session(martin).with(csrf()))
                .andExpect(flash().attributeExists("exito"));
        assertThat(estadoOrden(sinPagar)).isEqualTo(EstadoOrdenCompra.ANULADA);
        assertThat(estadoFactura(sinPagar)).isEqualTo(EstadoFactura.ANULADA);
        assertThat(stock(GORRA)).isEqualTo(gorrasAntes);

        // El admin confirma el pago de otra compra y el stock baja.
        String pagada = comprarGorraConTransferencia(martin);
        mvc.perform(post("/admin/pedidos/" + pagada + "/confirmar-pago").session(admin).with(csrf()))
                .andExpect(flash().attributeExists("exito"));
        assertThat(estadoFactura(pagada)).isEqualTo(EstadoFactura.PAGADA);
        assertThat(stock(GORRA)).isEqualTo(gorrasAntes - 1);

        // Ya pagada, el cliente no la puede anular; el admin sí, y la gorra vuelve al stock.
        mvc.perform(post("/cliente/compras/" + pagada + "/anular").session(martin).with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertThat(estadoOrden(pagada)).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);

        mvc.perform(post("/admin/pedidos/" + pagada + "/anular").session(admin).with(csrf())
                        .param("motivo", "El cliente pidió la devolución."))
                .andExpect(flash().attributeExists("exito"));
        assertThat(estadoOrden(pagada)).isEqualTo(EstadoOrdenCompra.ANULADA);
        assertThat(estadoFactura(pagada)).isEqualTo(EstadoFactura.ANULADA);
        assertThat(stock(GORRA)).isEqualTo(gorrasAntes);
    }

    @Test
    void unClienteNoPuedeEntrarAlPanelDeAdministracion() throws Exception {
        MockHttpSession martin = sesion(MARTIN, CLAVE_CLIENTES);

        mvc.perform(get("/admin").session(martin)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/pedidos").session(martin)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/pedidos/cualquiera/confirmar-pago").session(martin).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/compras/cualquiera/recibir").session(martin).with(csrf()))
                .andExpect(status().isForbidden());
        // Sin sesión, lo manda a iniciar sesión.
        mvc.perform(get("/admin")).andExpect(redirectedUrlPattern("**/login"));
    }

    private String comprarGorraConTransferencia(MockHttpSession cliente) throws Exception {
        mvc.perform(post("/cliente/carrito/agregar").session(cliente).with(csrf())
                .param("idProducto", idProducto(GORRA)).param("cantidad", "1"));
        String idOrden = carritoService.obtenerCarrito(idCliente(MARTIN)).getId();
        mvc.perform(post("/cliente/checkout").session(cliente).with(csrf())
                        .param("idFormaPago", idFormaDePago(TipoPago.TRANSFERENCIA)))
                .andExpect(redirectedUrl("/cliente/checkout/registrada/" + idOrden));
        return idOrden;
    }

    private MockHttpServletRequestBuilder login(String correo, String clave) {
        return post("/login").with(csrf()).param("username", correo).param("password", clave);
    }

    // Login real contra el formulario; la sesión queda autenticada para los requests siguientes.
    private MockHttpSession sesion(String correo, String clave) throws Exception {
        return (MockHttpSession) mvc.perform(login(correo, clave))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession();
    }

    private String idCliente(String correo) throws ErrorServiceException {
        return clienteService.buscarClientePorUsuario(usuarioService.buscarUsuarioPorNombre(correo).getId())
                .orElseThrow().getId();
    }

    private String idFormaDePago(TipoPago tipo) {
        return formaDePagoService.listarFormaDePagoActivo().stream()
                .filter(f -> f.getTipoPago() == tipo)
                .findFirst().orElseThrow().getId();
    }

    private String idProducto(String codigo) throws ErrorServiceException {
        return productoService.buscarProductoPorCodigo(codigo).getId();
    }

    private int stock(String codigo) throws ErrorServiceException {
        return stockService.buscarStockActual(idProducto(codigo));
    }

    private EstadoOrdenCompra estadoOrden(String idOrden) throws ErrorServiceException {
        return ordenCompraService.buscarPedido(idOrden).getEstadoOrdenCompra();
    }

    private EstadoFactura estadoFactura(String idOrden) {
        return ordenCompraService.buscarFacturaDePedido(idOrden).orElseThrow().getEstado();
    }
}

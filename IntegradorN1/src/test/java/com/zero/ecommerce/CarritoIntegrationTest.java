package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Stock;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.repositories.ClienteRepository;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.StockRepository;
import com.zero.ecommerce.repositories.SubCategoriaRepository;
import com.zero.ecommerce.repositories.UsuarioRepository;
import com.zero.ecommerce.repositories.VigenciaPrecioRepository;
import com.zero.ecommerce.services.CarritoService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class CarritoIntegrationTest {

    private static final String RUTA_CARRITO = "/cliente/carrito";
    private static final String CORREO_CLIENTE = "cliente@zero.com.ar";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private SubCategoriaRepository subCategoriaRepository;

    @Autowired
    private VigenciaPrecioRepository vigenciaPrecioRepository;

    @Autowired
    private StockRepository stockRepository;

    private Usuario usuario;
    private Cliente cliente;
    private Producto producto;

    @BeforeEach
    void setUp() {
        usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(CORREO_CLIENTE)
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setNombreUsuario(CORREO_CLIENTE);
                    u.setClave("DummyClave123!");
                    u.setRol(RolUsuario.CLIENTE);
                    return usuarioRepository.save(u);
                });

        cliente = clienteRepository.findByUsuario_IdAndEliminadoFalse(usuario.getId())
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setNombre("Test");
                    c.setApellido("Cliente");
                    c.setUsuario(usuario);
                    return clienteRepository.save(c);
                });

        SubCategoria subCategoria = subCategoriaRepository.findAll().stream().findFirst().orElse(null);

        producto = new Producto();
        producto.setNombre("Remera Running");
        producto.setCodigo("REM-001");
        producto.setTalle("L");
        producto.setSubCategoria(subCategoria);
        producto = productoRepository.save(producto);

        VigenciaPrecio vigencia = new VigenciaPrecio();
        vigencia.setProducto(producto);
        vigencia.setPrecio(12000.0);
        vigencia.setFechaDesde(LocalDate.now());
        vigenciaPrecioRepository.save(vigencia);

        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setCantidadActual(15);
        stock.setFecha(LocalDateTime.now());
        stockRepository.save(stock);
    }

    @Test
    void usuarioAnonimoEsRedirigidoAlLogin() throws Exception {
        mvc.perform(get(RUTA_CARRITO))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = CORREO_CLIENTE, roles = { "CLIENTE" })
    void clienteAutenticadoPuedeVerSuCarrito() throws Exception {
        mvc.perform(get(RUTA_CARRITO))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = CORREO_CLIENTE, roles = { "CLIENTE" })
    void flujoCompletoAgregarModificarYQuitar() throws Exception {
        MockHttpSession sesion = loginSesion();
        CsrfToken csrf = csrf(sesion);

        // 1. Agregar producto al carrito
        mvc.perform(post(RUTA_CARRITO + "/agregar")
                .session(sesion)
                .param(csrf.getParameterName(), csrf.getToken())
                .param("idProducto", producto.getId())
                .param("cantidad", "2"))
                .andExpect(status().is3xxRedirection());

        OrdenCompra carrito = carritoService.obtenerCarrito(cliente.getId());
        assertThat(carrito.getDetalles()).hasSize(1);
        assertThat(carrito.getDetalles().get(0).getCantidad()).isEqualTo(2);
        assertThat(carrito.getTotal()).isEqualTo(24000.0);

        String idDetalle = carrito.getDetalles().get(0).getId();

        // 2. Modificar cantidad
        mvc.perform(post(RUTA_CARRITO + "/cantidad")
                .session(sesion)
                .param(csrf.getParameterName(), csrf.getToken())
                .param("idDetalle", idDetalle)
                .param("cantidad", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(RUTA_CARRITO));

        carrito = carritoService.obtenerCarrito(cliente.getId());
        assertThat(carrito.getDetalles().get(0).getCantidad()).isEqualTo(4);
        assertThat(carrito.getTotal()).isEqualTo(48000.0);

        // 3. Quitar producto
        mvc.perform(post(RUTA_CARRITO + "/quitar")
                .session(sesion)
                .param(csrf.getParameterName(), csrf.getToken())
                .param("idDetalle", idDetalle))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(RUTA_CARRITO));

        carrito = carritoService.obtenerCarrito(cliente.getId());
        assertThat(carrito.getDetalles()).isEmpty();
        assertThat(carrito.getTotal()).isZero();
    }

    @Test
    @WithMockUser(username = CORREO_CLIENTE, roles = { "CLIENTE" })
    void carritoPersisteEntreSesiones() throws Exception {
        // En una primera sesión agrega productos
        carritoService.agregarProducto(cliente.getId(), producto.getId(), 3);

        // Se verifica en el servicio que al cerrar sesión y volver a consultar, los datos persisten
        OrdenCompra carrito = carritoService.obtenerCarrito(cliente.getId());
        assertThat(carrito.getDetalles()).hasSize(1);
        assertThat(carrito.getDetalles().get(0).getCantidad()).isEqualTo(3);
        assertThat(carrito.getTotal()).isEqualTo(36000.0);
    }

    private MockHttpSession loginSesion() throws Exception {
        MvcResult getResult = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) getResult.getRequest().getSession(false);
        CsrfToken csrf = (CsrfToken) getResult.getRequest().getAttribute(CsrfToken.class.getName());

        mvc.perform(post("/login")
                .session(session)
                .param(csrf.getParameterName(), csrf.getToken())
                .param("username", CORREO_CLIENTE)
                .param("password", "Cliente123!"))
                .andExpect(status().is3xxRedirection());

        return session;
    }

    private CsrfToken csrf(MockHttpSession session) throws Exception {
        MvcResult res = mvc.perform(get("/cliente").session(session)).andReturn();
        return (CsrfToken) res.getRequest().getAttribute(CsrfToken.class.getName());
    }
}

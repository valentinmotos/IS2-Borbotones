package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Stock;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.repositories.CategoriaRepository;
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
class CarritoSitioIntegrationTest {

    private static final String CORREO_CLIENTE = "cliente.sitio@zero.com.ar";
    private static final String CLAVE_CLIENTE = "Cliente123!";
    private static final String CORREO_EMPLEADO = "empleado.sitio@zero.com.ar";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private SubCategoriaRepository subCategoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VigenciaPrecioRepository vigenciaPrecioRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario usuarioCliente;
    private Cliente cliente;
    private Producto producto;
    private String catalogoUrl;
    private String productoUrl;

    @BeforeEach
    void setUp() {
        usuarioCliente = usuarioRepository.findByNombreUsuarioIgnoreCase(CORREO_CLIENTE)
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setNombreUsuario(CORREO_CLIENTE);
                    u.setClave(passwordEncoder.encode(CLAVE_CLIENTE));
                    u.setRol(RolUsuario.CLIENTE);
                    return usuarioRepository.save(u);
                });

        cliente = clienteRepository.findByUsuario_IdAndEliminadoFalse(usuarioCliente.getId())
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setNombre("Cliente");
                    c.setApellido("Sitio");
                    c.setUsuario(usuarioCliente);
                    return clienteRepository.save(c);
                });

        usuarioRepository.findByNombreUsuarioIgnoreCase(CORREO_EMPLEADO)
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setNombreUsuario(CORREO_EMPLEADO);
                    u.setClave(passwordEncoder.encode("Empleado123!"));
                    u.setRol(RolUsuario.ADMINISTRATIVO);
                    return usuarioRepository.save(u);
                });

        Categoria categoria = categoriaRepository.findAll().stream().findFirst().orElseGet(() -> {
            Categoria cat = new Categoria();
            cat.setNombre("Indumentaria");
            return categoriaRepository.save(cat);
        });

        SubCategoria subCategoria = subCategoriaRepository.findAll().stream().findFirst().orElseGet(() -> {
            SubCategoria sub = new SubCategoria();
            sub.setNombre("Camperas");
            sub.setCategoria(categoria);
            return subCategoriaRepository.save(sub);
        });

        producto = new Producto();
        producto.setNombre("Campera Invierno Zero");
        producto.setCodigo("CAMP-01");
        producto.setTalle("XL");
        producto.setSubCategoria(subCategoria);
        producto = productoRepository.save(producto);
        catalogoUrl = "/catalogo/" + subCategoria.getCategoria().getId();
        productoUrl = "/producto/" + producto.getId();

        VigenciaPrecio vigencia = new VigenciaPrecio();
        vigencia.setProducto(producto);
        vigencia.setPrecio(25000.0);
        vigencia.setFechaDesde(LocalDate.now());
        vigenciaPrecioRepository.save(vigencia);

        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setCantidadActual(10);
        stock.setFecha(LocalDateTime.now());
        stockRepository.save(stock);
    }

    @Test
    void visitanteAnonimoVeContadorEnCero() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-notify=\"0\"")));
    }

    @Test
    @WithMockUser(username = CORREO_CLIENTE, roles = { "CLIENTE" })
    void clienteLogueadoVeContadorActualizadoConItems() throws Exception {
        // Inicialmente el contador es 0
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-notify=\"0\"")));

        // Agregamos 2 unidades
        carritoService.agregarProducto(cliente.getId(), producto.getId(), 2);

        // Ahora el contador en la home/header debe ser 2
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-notify=\"2\"")));
    }

    @Test
    @WithMockUser(username = CORREO_EMPLEADO, roles = { "ADMINISTRATIVO" })
    void empleadoVeBotonCarritoDeshabilitado() throws Exception {
        mvc.perform(get(productoUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Solo clientes")));
    }

    @Test
    void flujoCompletoVisitanteAgregaVaALoginVuelveConItemAgregadoYContadorActualizado() throws Exception {
        // 1. Visitante navega al detalle del producto
        MvcResult getProducto = mvc.perform(get(productoUrl)).andExpect(status().isOk()).andReturn();
        MockHttpSession sesion = (MockHttpSession) getProducto.getRequest().getSession(false);
        assertThat(getProducto.getResponse().getContentAsString()).contains("action=\"/login\"")
                .contains("name=\"retorno\" value=\"" + productoUrl + "\"");

        // 2. Toca "Agregar al carrito" y el login guarda el producto pendiente
        MvcResult getLogin = mvc.perform(get("/login")
                .session(sesion)
                .param("idProducto", producto.getId())
                .param("cantidad", "2")
                .param("retorno", productoUrl))
                .andExpect(status().isOk())
                .andReturn();

        // 3. Se loguea con credenciales de cliente
        CsrfToken loginCsrf = (CsrfToken) getLogin.getRequest().getAttribute(CsrfToken.class.getName());

        MvcResult loginResult = mvc.perform(post("/login")
                .session(sesion)
                .param(loginCsrf.getParameterName(), loginCsrf.getToken())
                .param("username", CORREO_CLIENTE)
                .param("password", CLAVE_CLIENTE))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(loginResult.getResponse().getRedirectedUrl()).contains(productoUrl);

        // 4. Verifica que en el carrito del cliente ahora está el producto
        var carrito = carritoService.obtenerCarrito(cliente.getId());
        assertThat(carrito.getDetalles()).hasSize(1);
        assertThat(carrito.getDetalles().get(0).getCantidad()).isEqualTo(2);

        // 5. Al volver al producto, el contador del header ahora muestra 2
        mvc.perform(get(productoUrl).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-notify=\"2\"")))
                .andExpect(content().string(containsString("Ver carrito")));
    }

    @Test
    void flujoVisitanteConParametroRetornoEnLogin() throws Exception {
        // 1. Visitante accede a login con parámetros de retorno y producto
        MvcResult resLogin = mvc.perform(get("/login")
                .param("retorno", catalogoUrl)
                .param("idProducto", producto.getId())
                .param("cantidad", "3"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession sesion = (MockHttpSession) resLogin.getRequest().getSession(false);
        assertThat(sesion).isNotNull();

        // 2. Se autentica
        CsrfToken csrf = (CsrfToken) resLogin.getRequest().getAttribute(CsrfToken.class.getName());
        MvcResult loginResult = mvc.perform(post("/login")
                .session(sesion)
                .param(csrf.getParameterName(), csrf.getToken())
                .param("username", CORREO_CLIENTE)
                .param("password", CLAVE_CLIENTE))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(loginResult.getResponse().getRedirectedUrl()).contains(catalogoUrl);

        // 3. Ítem agregado y contador en 3
        var carrito = carritoService.obtenerCarrito(cliente.getId());
        assertThat(carrito.getDetalles()).hasSize(1);
        assertThat(carrito.getDetalles().get(0).getCantidad()).isEqualTo(3);
    }
}

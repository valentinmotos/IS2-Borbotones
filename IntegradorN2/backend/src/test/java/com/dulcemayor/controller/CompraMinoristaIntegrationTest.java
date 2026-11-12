package com.dulcemayor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dulcemayor.dto.PuntoDeVentaRequest;
import com.dulcemayor.dto.ResultadoFacturacionResponse;
import com.dulcemayor.entity.DetallePedido;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.repository.LocalidadRepository;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.PuntoDeVentaRepository;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.JwtService;
import com.dulcemayor.service.FacturacionService;
import com.dulcemayor.service.PedidoService;
import com.dulcemayor.service.PuntoDeVentaService;
import com.dulcemayor.service.StockService;
import com.dulcemayor.service.UsuarioService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * E4-08: criterio de aceptación de punta a punta. El minorista ve todos sus pedidos, descarga cada
 * factura y no puede ver pedidos ni facturas de otro minorista.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:comprasminorista;MODE=PostgreSQL;"
        + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompraMinoristaIntegrationTest {
    private static final String OTRO_MINORISTA = "otro.minorista@dulcemayor.com.ar";

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwt;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private UsuarioService usuarioService;
    @Autowired private MinoristaRepository minoristas;
    @Autowired private PreventorRepository preventores;
    @Autowired private PuntoDeVentaRepository puntosDeVenta;
    @Autowired private PuntoDeVentaService puntoDeVentaService;
    @Autowired private LocalidadRepository localidades;
    @Autowired private ProductoRepository productos;
    @Autowired private PedidoService pedidos;
    @Autowired private FacturacionService facturacion;
    @Autowired private StockService stock;
    @MockitoBean private JavaMailSender mailSender;

    @BeforeEach
    void prepararCorreo() {
        // E4-07 envía la factura por correo al facturar; acá solo hace falta que no falle.
        when(mailSender.createMimeMessage()).thenAnswer(invocacion -> new MimeMessage((Session) null));
    }

    @Test
    void elMinoristaVeSusPedidosDescargaSusFacturasYNoVeLosDeOtro() throws Exception {
        Producto producto = productos.findByEliminadoFalse().getFirst();
        stock.reponer(producto.getId(), 20, "Ingreso para el test de compras del minorista.");
        PuntoDeVenta kioscoDemo = puntoDelPreventor("Kiosco Centro");
        PuntoDeVenta kioscoAjeno = puntoDeOtroMinorista();

        String facturado = cargar(4801, kioscoDemo, producto, 2).getId();
        String sinFacturar = cargar(4802, kioscoDemo, producto, 1).getId();
        String ajeno = cargar(4803, kioscoAjeno, producto, 1).getId();
        ResultadoFacturacionResponse factura = facturacion.facturarPedido(facturado, usuario("administrativo").getId());
        ResultadoFacturacionResponse facturaAjena = facturacion.facturarPedido(ajeno, usuario("administrativo").getId());
        String facturaId = factura.factura().id();
        String facturaAjenaId = facturaAjena.factura().id();
        String minorista = bearer("minorista@dulcemayor.com.ar");

        // Ve todos sus pedidos (con y sin factura) y ninguno del otro minorista.
        mvc.perform(get("/api/minorista/compras").header(HttpHeaders.AUTHORIZATION, minorista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pedidoId == '%s')].facturaId".formatted(facturado)).value(facturaId))
                .andExpect(jsonPath("$[?(@.pedidoId == '%s')].estadoFactura".formatted(facturado))
                        .value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$[?(@.pedidoId == '%s')].puedePagar".formatted(facturado)).value(true))
                .andExpect(jsonPath("$[?(@.pedidoId == '%s')].estadoPedido".formatted(sinFacturar)).value("CARGADO"))
                .andExpect(jsonPath("$[?(@.pedidoId == '%s')].puedePagar".formatted(sinFacturar)).value(false))
                .andExpect(jsonPath("$[?(@.pedidoId == '%s')]".formatted(ajeno)).isEmpty());

        // Detalle con líneas y línea de tiempo CARGADO → FACTURADO.
        mvc.perform(get("/api/minorista/compras/" + facturado).header(HttpHeaders.AUTHORIZATION, minorista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compra.numeroPedido").value(4801))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2))
                .andExpect(jsonPath("$.lineaDeTiempo[0].estado").value("CARGADO"))
                .andExpect(jsonPath("$.lineaDeTiempo[1].estado").value("FACTURADO"))
                .andExpect(jsonPath("$.lineaDeTiempo[0].usuario").doesNotExist());

        // Descarga su factura.
        byte[] pdf = mvc.perform(get("/api/minorista/facturas/" + facturaId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, minorista))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"factura-%08d.pdf\"".formatted(factura.factura().numeroFactura())))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        // El pedido y la factura de otro minorista responden 404 aunque se conozca el id.
        mvc.perform(get("/api/minorista/compras/" + ajeno).header(HttpHeaders.AUTHORIZATION, minorista))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No se encontró el pedido."));
        mvc.perform(get("/api/minorista/facturas/" + facturaAjenaId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, minorista))
                .andExpect(status().isNotFound());

        // Y el otro minorista solo ve el suyo.
        mvc.perform(get("/api/minorista/compras").header(HttpHeaders.AUTHORIZATION, bearer(OTRO_MINORISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pedidoId").value(ajeno));
    }

    @Test
    void soloElMinoristaUsaElPanelDeCompras() throws Exception {
        mvc.perform(get("/api/minorista/compras")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/minorista/compras")
                        .header(HttpHeaders.AUTHORIZATION, bearer("preventor@dulcemayor.com.ar")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/minorista/facturas/cualquiera/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer("administrativo@dulcemayor.com.ar")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/minorista/compras/inexistente")
                        .header(HttpHeaders.AUTHORIZATION, bearer("minorista@dulcemayor.com.ar")))
                .andExpect(status().isNotFound());
    }

    private PuntoDeVenta puntoDelPreventor(String nombreComercio) {
        var preventor = preventores.findByUsuario_IdAndEliminadoFalse(usuario("preventor").getId()).orElseThrow();
        return puntosDeVenta.findByPreventor_IdAndEliminadoFalseOrderByNombreComercioAsc(preventor.getId()).stream()
                .filter(p -> p.getNombreComercio().equals(nombreComercio)).findFirst().orElseThrow();
    }

    /** Segundo minorista con su propio kiosco, atendido por el mismo preventor de demo. */
    private PuntoDeVenta puntoDeOtroMinorista() {
        Usuario cuenta = usuarios.findByNombreUsuarioAndEliminadoFalse(OTRO_MINORISTA)
                .orElseGet(() -> usuarioService.crearUsuario(OTRO_MINORISTA, RolUsuario.MINORISTA,
                        "claveSegura123", false));
        Minorista otro = minoristas.findByUsuario_IdAndEliminadoFalse(cuenta.getId()).orElseGet(() -> {
            Minorista nuevo = new Minorista();
            nuevo.setNombre("Olga");
            nuevo.setApellido("Otra");
            nuevo.setFechaNacimiento(LocalDate.of(1980, 5, 5));
            nuevo.setTipoDocumento(TipoDocumento.DNI);
            nuevo.setNumeroDocumento("30000099");
            nuevo.setUsuario(cuenta);
            return minoristas.save(nuevo);
        });
        var localidad = localidades.findByEliminadoFalse(Sort.by("nombre")).getFirst();
        String id = puntoDeVentaService.crearPuntoDeVenta(new PuntoDeVentaRequest("Kiosco Ajeno", otro.getId(),
                localidad.getId(), "Las Heras", "100", null, null, null, null, -32.89, -68.84),
                usuario("preventor").getId()).id();
        return puntosDeVenta.findById(id).orElseThrow();
    }

    private Pedido cargar(long numero, PuntoDeVenta punto, Producto producto, int cantidad) {
        Usuario usuario = usuario("preventor");
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(numero);
        pedido.setPreventor(preventores.findByUsuario_IdAndEliminadoFalse(usuario.getId()).orElseThrow());
        pedido.setPuntoDeVenta(punto);
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(1000);
        detalle.setSubtotal(cantidad * 1000.0);
        pedido.agregarDetalle(detalle);
        pedido.setSubtotal(cantidad * 1000.0);
        pedido.setTotal(cantidad * 1000.0);
        return pedidos.registrarCarga(pedido, usuario.getId(), null);
    }

    private Usuario usuario(String alias) {
        String correo = alias.contains("@") ? alias : alias + "@dulcemayor.com.ar";
        return usuarios.findByNombreUsuarioAndEliminadoFalse(correo).orElseThrow();
    }

    private String bearer(String correo) {
        return "Bearer " + jwt.emitir(usuario(correo));
    }
}

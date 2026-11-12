package com.dulcemayor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dulcemayor.dto.HistorialEstadoPedidoResponse;
import com.dulcemayor.entity.DetallePedido;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.repository.PedidoRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.PuntoDeVentaRepository;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.JwtService;
import com.dulcemayor.service.PedidoService;
import com.dulcemayor.service.StockService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** E4-04: criterio de aceptación de punta a punta (pedido con stock → factura, stock descontado, PDF). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:facturacion;MODE=PostgreSQL;"
        + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FacturacionIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JwtService jwt;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PreventorRepository preventores;
    @Autowired private PuntoDeVentaRepository puntosDeVenta;
    @Autowired private ProductoRepository productos;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private PedidoService pedidos;
    @Autowired private StockService stock;
    @MockitoBean private JavaMailSender mailSender;

    private final ConcurrentMap<String, CompletableFuture<MimeMessage>> correos = new ConcurrentHashMap<>();

    @BeforeEach
    void prepararCorreo() {
        when(mailSender.createMimeMessage()).thenAnswer(invocacion -> new MimeMessage((Session) null));
        doAnswer(invocacion -> {
            MimeMessage correo = invocacion.getArgument(0);
            correoPorAsunto(correo.getSubject()).complete(correo);
            return null;
        }).when(mailSender).send(ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void unPedidoConStockSeFacturaDescuentaElStockYDejaLaFacturaDescargable() throws Exception {
        String administrativo = bearer("administrativo");
        Producto producto = productos.findByEliminadoFalse().getFirst();
        stock.reponer(producto.getId(), 20, "Ingreso para el test de facturación.");
        int saldoInicial = saldo(producto);
        String primero = cargar(4401, producto, 3).getId();
        String segundo = cargar(4402, producto, 2).getId();

        JsonNode resultado = leer(mvc.perform(post("/api/pedidos/" + primero + "/facturar")
                        .header(HttpHeaders.AUTHORIZATION, administrativo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPedido").value("FACTURADO"))
                .andExpect(jsonPath("$.faltantes.length()").value(0))
                .andExpect(jsonPath("$.factura.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.factura.detalles[0].cantidad").value(3))
                .andReturn().getResponse().getContentAsString());
        String facturaId = resultado.at("/factura/id").asText();
        long numero = resultado.at("/factura/numeroFactura").asLong();

        assertThat(saldo(producto)).isEqualTo(saldoInicial - 3);
        assertThat(pedidos.consultarHistorial(primero)).extracting(HistorialEstadoPedidoResponse::estado)
                .containsExactly(EstadoPedido.CARGADO, EstadoPedido.FACTURADO);
        mvc.perform(get("/api/pedidos/" + primero + "/factura").header(HttpHeaders.AUTHORIZATION, administrativo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(facturaId));

        byte[] pdf = mvc.perform(get("/api/facturas/" + facturaId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, administrativo))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"factura-%08d.pdf\"".formatted(numero)))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        verificarCorreo(numero, producto.getNombre());

        // Numeración secuencial y un pedido no se factura dos veces.
        mvc.perform(post("/api/pedidos/" + segundo + "/facturar").header(HttpHeaders.AUTHORIZATION, administrativo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.factura.numeroFactura").value(numero + 1));
        mvc.perform(post("/api/pedidos/" + primero + "/facturar").header(HttpHeaders.AUTHORIZATION, administrativo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campo").value("estado"));
        assertThat(saldo(producto)).isEqualTo(saldoInicial - 5);
        mvc.perform(get("/api/facturas").param("estado", "PENDIENTE_PAGO")
                        .header(HttpHeaders.AUTHORIZATION, administrativo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].numeroPedido".formatted(facturaId)).value(4401));
    }

    private void verificarCorreo(long numero, String producto) throws Exception {
        String asunto = "Factura N° %08d · Dulce Mayor".formatted(numero);
        MimeMessage correo = correoPorAsunto(asunto).get(5, TimeUnit.SECONDS);
        correo.saveChanges();
        assertThat(correo.getSubject()).isEqualTo(asunto);
        assertThat(correo.getAllRecipients()).extracting(Object::toString)
                .containsExactly("minorista@dulcemayor.com.ar");

        MimeMultipart mensaje = (MimeMultipart) correo.getContent();
        var html = ((MimeMultipart) mensaje.getBodyPart(0).getContent()).getBodyPart(0);
        assertThat(html.isMimeType("text/html")).isTrue();
        assertThat(html.getContent().toString()).contains("4401", producto, "3.000,00");

        var adjunto = mensaje.getBodyPart(1);
        assertThat(adjunto.getFileName()).isEqualTo("factura-%08d.pdf".formatted(numero));
        assertThat(adjunto.isMimeType("application/pdf")).isTrue();
        try (var contenido = adjunto.getInputStream()) {
            assertThat(new String(contenido.readNBytes(5), StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        }
    }

    private CompletableFuture<MimeMessage> correoPorAsunto(String asunto) {
        return correos.computeIfAbsent(asunto, clave -> new CompletableFuture<>());
    }

    @Test
    void sinStockQuedaEnSinStockYSeFacturaAlProcesarLuegoDeReponer() throws Exception {
        String jefe = bearer("jefe");
        Producto producto = productos.findByEliminadoFalse().getLast();
        int saldoInicial = saldo(producto);
        int pedida = saldoInicial + 7;
        String id = cargar(4403, producto, pedida).getId();

        mvc.perform(post("/api/pedidos/" + id + "/facturar").header(HttpHeaders.AUTHORIZATION, jefe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPedido").value("SIN_STOCK"))
                .andExpect(jsonPath("$.factura").doesNotExist())
                .andExpect(jsonPath("$.faltantes[0].productoId").value(producto.getId()))
                .andExpect(jsonPath("$.faltantes[0].cantidadPedida").value(pedida))
                .andExpect(jsonPath("$.faltantes[0].stockDisponible").value(saldoInicial));
        assertThat(saldo(producto)).isEqualTo(saldoInicial);
        mvc.perform(get("/api/pedidos/" + id + "/factura").header(HttpHeaders.AUTHORIZATION, jefe))
                .andExpect(status().isNotFound());

        stock.reponer(producto.getId(), 7, "Llegó la mercadería que faltaba.");
        mvc.perform(post("/api/pedidos/procesar").header(HttpHeaders.AUTHORIZATION, jefe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pedidoId == '%s')].estadoPedido".formatted(id)).value("FACTURADO"));
        assertThat(pedidos.consultarHistorial(id)).extracting(HistorialEstadoPedidoResponse::estado)
                .containsExactly(EstadoPedido.CARGADO, EstadoPedido.SIN_STOCK, EstadoPedido.FACTURADO);
    }

    @Test
    void soloGestionPuedeFacturarYDescargarFacturas() throws Exception {
        String preventor = bearer("preventor");
        mvc.perform(post("/api/pedidos/cualquiera/facturar").header(HttpHeaders.AUTHORIZATION, preventor))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/pedidos/procesar").header(HttpHeaders.AUTHORIZATION, preventor))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/facturas").header(HttpHeaders.AUTHORIZATION, preventor))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/facturas/cualquiera/pdf").header(HttpHeaders.AUTHORIZATION, preventor))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/facturas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/pedidos/inexistente/facturar").header(HttpHeaders.AUTHORIZATION, bearer("administrativo")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/facturas/inexistente/pdf").header(HttpHeaders.AUTHORIZATION, bearer("administrativo")))
                .andExpect(status().isNotFound());
    }

    private Pedido cargar(long numero, Producto producto, int cantidad) {
        Usuario usuario = usuario("preventor");
        var preventor = preventores.findByUsuario_IdAndEliminadoFalse(usuario.getId()).orElseThrow();
        var punto = puntosDeVenta.findByPreventor_IdAndEliminadoFalseOrderByNombreComercioAsc(preventor.getId())
                .getFirst();
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(numero);
        pedido.setPreventor(preventor);
        pedido.setPuntoDeVenta(punto);
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(1000);
        detalle.setSubtotal(cantidad * 1000.0);
        pedido.agregarDetalle(detalle);
        pedido.setSubtotal(cantidad * 1000.0);
        Pedido guardado = pedidos.registrarCarga(pedido, usuario.getId(), null);
        assertThat(pedidoRepository.findById(guardado.getId())).isPresent();
        return guardado;
    }

    private int saldo(Producto producto) {
        return stock.buscarStockPorProducto(producto.getId()).cantidadActual();
    }

    private JsonNode leer(String respuesta) throws Exception {
        return json.readTree(respuesta);
    }

    private Usuario usuario(String alias) {
        return usuarios.findByNombreUsuarioAndEliminadoFalse(alias + "@dulcemayor.com.ar").orElseThrow();
    }

    private String bearer(String alias) {
        return "Bearer " + jwt.emitir(usuario(alias));
    }
}

package com.dulcemayor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dulcemayor.dto.PromocionRequest;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.repository.DetallePedidoRepository;
import com.dulcemayor.repository.HistorialEstadoPedidoRepository;
import com.dulcemayor.repository.PedidoRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.JwtService;
import com.dulcemayor.service.CatalogoService;
import com.dulcemayor.service.PedidoService;
import com.dulcemayor.service.PromocionService;
import com.dulcemayor.service.StockService;
import com.dulcemayor.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PreventaIntegrationTest {
    private static final String PREVENTA = "/api/preventa";
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JwtService jwt;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private UsuarioService usuarioService;
    @Autowired private PreventorRepository preventores;
    @Autowired private CatalogoService catalogo;
    @Autowired private StockService stock;
    @Autowired private PedidoService estados;
    @Autowired private PedidoRepository pedidos;
    @Autowired private DetallePedidoRepository detalles;
    @Autowired private HistorialEstadoPedidoRepository historial;
    @Autowired private PromocionService promociones;

    @Test
    void registraMinoristaYPuntoYCargaPedidoConStockYPreciosDelServidor() throws Exception {
        String preventor = token("preventor");
        String otro = crearOtroPreventor();
        String administrativo = token("administrativo");
        String minorista = id(mvc.perform(post(PREVENTA + "/minoristas").header("Authorization", preventor)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Inés","apellido":"Prueba","fechaNacimiento":"1990-01-01",
                                 "tipoDocumento":"DNI","numeroDocumento":"32126802",
                                 "correo":"e4-02-alta@example.invalid","clave":"Prueba1234!"}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        var departamentos = mvc.perform(get("/api/departamentos").header("Authorization", preventor))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String departamento = json.readTree(departamentos).get(0).get("id").asText();
        var localidades = mvc.perform(get("/api/departamentos/" + departamento + "/localidades")
                        .header("Authorization", preventor))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String localidad = json.readTree(localidades).get(0).get("id").asText();
        String punto = id(mvc.perform(post("/api/puntos-de-venta").header("Authorization", preventor)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombreComercio":"Comercio E4-02","minoristaId":"%s","localidadId":"%s",
                                 "calle":"San Martín","numeracion":"100","latitud":-32.89,"longitud":-68.84}
                                """.formatted(minorista, localidad)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(get(PREVENTA + "/puntos-de-venta").header("Authorization", preventor))
                .andExpect(jsonPath("$[?(@.id == '%s')].nombreComercio".formatted(punto)).value("Comercio E4-02"));
        mvc.perform(get(PREVENTA + "/puntos-de-venta").header("Authorization", otro))
                .andExpect(jsonPath("$.length()").value(0));

        var producto = catalogo.buscar(null, null).stream().filter(p -> p.stock() >= 2).findFirst().orElseThrow();
        mvc.perform(get("/api/publico/catalogo/" + producto.id()))
                .andExpect(jsonPath("$.stock").value(producto.stock()));
        mvc.perform(get("/api/stock/" + producto.id()).header("Authorization", administrativo))
                .andExpect(jsonPath("$.cantidadActual").value(producto.stock()));
        // NOTA (E4-04): desde E4-03 el código se valida y aplica de verdad; antes este test mandaba
        // PROMO-E4 sin crearla y el código se ignoraba. Se crea una promo por volumen del 10 %.
        promociones.crear(new PromocionRequest("PROMO-E4", "Promo del test de preventa", 10.0, 1, null,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
        var subtotal = BigDecimal.valueOf(producto.precio()).multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP);
        var total = subtotal.subtract(subtotal.multiply(BigDecimal.TEN).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        String solicitud = pedido(punto, producto.id(), 2, UUID.randomUUID());
        mvc.perform(post(PREVENTA + "/pedidos").header("Authorization", otro)
                        .contentType(MediaType.APPLICATION_JSON).content(solicitud))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campo").value("puntoDeVentaId"));
        String pedidoId = id(mvc.perform(post(PREVENTA + "/pedidos").header("Authorization", preventor)
                        .contentType(MediaType.APPLICATION_JSON).content(solicitud))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("CARGADO"))
                .andExpect(jsonPath("$.total").value(total.doubleValue()))
                .andExpect(jsonPath("$.codigoPromocion").value("PROMO-E4"))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(post(PREVENTA + "/pedidos").header("Authorization", preventor)
                        .contentType(MediaType.APPLICATION_JSON).content(solicitud))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(pedidoId));
        assertThat(detalles.findByPedido_IdAndEliminadoFalse(pedidoId)).singleElement().satisfies(detalle -> {
            assertThat(detalle.getCantidad()).isEqualTo(2);
            assertThat(detalle.getPrecioUnitario()).isEqualTo(producto.precio());
        });
        assertThat(historial.findByPedido_IdOrderByFechaHoraAsc(pedidoId))
                .singleElement().satisfies(registro -> assertThat(registro.getEstado()).isEqualTo(EstadoPedido.CARGADO));
        assertThat(stock.buscarStockPorProducto(producto.id()).cantidadActual()).isEqualTo(producto.stock());

        estados.avanzar(pedidoId, EventoPedido.MARCAR_SIN_STOCK, usuario("administrativo").getId(), null, null);
        mvc.perform(get(PREVENTA + "/pedidos").header("Authorization", preventor))
                .andExpect(jsonPath("$[?(@.id == '%s')].estado".formatted(pedidoId)).value("SIN_STOCK"));
        mvc.perform(get(PREVENTA + "/pedidos").header("Authorization", otro))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(PREVENTA + "/pedidos").header("Authorization", administrativo))
                .andExpect(status().isForbidden());
        mvc.perform(get(PREVENTA + "/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaUnSaldoDesactualizadoYNoGuardaUnPedidoParcial() throws Exception {
        String preventor = token("preventor");
        String respuesta = mvc.perform(get(PREVENTA + "/puntos-de-venta").header("Authorization", preventor))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String punto = json.readTree(respuesta).get(0).get("id").asText();
        var producto = catalogo.buscar(null, null).stream().filter(p -> p.stock() > 1).findFirst().orElseThrow();
        long cantidadPedidos = pedidos.count();
        int diferencia = producto.stock() - 1;
        stock.descontar(producto.id(), diferencia);
        try {
            mvc.perform(get("/api/publico/catalogo/" + producto.id())).andExpect(jsonPath("$.stock").value(1));
            mvc.perform(post(PREVENTA + "/pedidos").header("Authorization", preventor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(pedido(punto, producto.id(), 2, UUID.randomUUID())))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campo").value("lineas"));
            assertThat(pedidos.count()).isEqualTo(cantidadPedidos);
        } finally {
            stock.reponer(producto.id(), diferencia, null);
        }
    }

    private String pedido(String punto, String producto, int cantidad, UUID clave) {
        return """
                {"claveCarga":"%s","puntoDeVentaId":"%s","codigoPromocion":" PROMO-E4 ",
                 "lineas":[{"productoId":"%s","cantidad":%d}]}
                """.formatted(clave, punto, producto, cantidad);
    }

    private String id(String respuesta) throws Exception {
        return json.readTree(respuesta).get("id").asText();
    }

    private Usuario usuario(String alias) {
        return usuarios.findByNombreUsuarioAndEliminadoFalse(alias + "@dulcemayor.com.ar").orElseThrow();
    }

    private String token(String alias) {
        return "Bearer " + jwt.emitir(usuario(alias));
    }

    private String crearOtroPreventor() {
        var usuario = usuarioService.crearUsuario("otro-e402@example.invalid", RolUsuario.PREVENTOR, "Prueba1234!", false);
        var preventor = new Preventor();
        preventor.setNombre("Otro");
        preventor.setApellido("Preventor");
        preventor.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        preventor.setTipoDocumento(TipoDocumento.DNI);
        preventor.setNumeroDocumento("32126803");
        preventor.setLegajo("P-E4-02");
        preventor.setUsuario(usuario);
        preventores.save(preventor);
        return "Bearer " + jwt.emitir(usuario);
    }
}

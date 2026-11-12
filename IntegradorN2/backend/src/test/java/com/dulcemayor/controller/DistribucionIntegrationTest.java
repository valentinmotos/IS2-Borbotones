package com.dulcemayor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dulcemayor.dto.HistorialEstadoPedidoResponse;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.repository.DistribuidorRepository;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.PuntoDeVentaRepository;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.JwtService;
import com.dulcemayor.service.EmailService;
import com.dulcemayor.service.PedidoService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * E5-02: criterio de aceptación de punta a punta. Marcar un pedido como entregado lo actualiza en el
 * historial (fecha y transportista) y al siguiente punto le llega el correo de "en camino".
 * El correo se reemplaza por un mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DistribucionIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwt;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PreventorRepository preventores;
    @Autowired private PuntoDeVentaRepository puntosDeVenta;
    @Autowired private DistribuidorRepository distribuidores;
    @Autowired private FacturaRepository facturas;
    @Autowired private PedidoService pedidos;

    @MockitoBean private EmailService correo;

    @Test
    void entregarActualizaElHistorialYAvisaAlSiguientePunto() throws Exception {
        Usuario distribuidor = usuario("distribuidor");
        Usuario administrativo = usuario("administrativo");
        String transportistaId = distribuidores.findByUsuario_IdAndEliminadoFalse(distribuidor.getId())
                .orElseThrow().getId();
        List<PuntoDeVenta> puntos = puntosDelPreventor();
        PuntoDeVenta centro = porNombre(puntos, "Kiosco Centro");
        PuntoDeVenta godoyCruz = porNombre(puntos, "Almacén Godoy Cruz");
        String primero = facturadoYPagado(5201, centro, administrativo);
        String segundo = facturadoYPagado(5202, godoyCruz, administrativo);
        String sinPagar = facturado(5203, centro, administrativo, EstadoFactura.PENDIENTE_PAGO);

        mvc.perform(get("/api/distribucion/pedidos-listos").header(HttpHeaders.AUTHORIZATION, bearer(distribuidor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + primero + "')]").exists())
                .andExpect(jsonPath("$[?(@.id == '" + segundo + "')]").exists())
                .andExpect(jsonPath("$[?(@.id == '" + sinPagar + "')]").doesNotExist());

        mvc.perform(post("/api/distribucion/reparto").header(HttpHeaders.AUTHORIZATION, bearer(distribuidor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoIds\":[\"" + sinPagar + "\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El pedido N° 5203 todavía no está pagado."));

        mvc.perform(post("/api/distribucion/reparto").header(HttpHeaders.AUTHORIZATION, bearer(distribuidor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoIds\":[\"" + primero + "\",\"" + segundo + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distribuidorId").value(transportistaId))
                .andExpect(jsonPath("$.paradas[?(@.pedidoId == '" + primero + "')].estadoPedido").value("EN_REPARTO"));

        mvc.perform(post("/api/distribucion/pedidos/" + primero + "/entrega")
                        .header(HttpHeaders.AUTHORIZATION, bearer(distribuidor))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacion\":\"Recibió Marta.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historial.estado").value("ENTREGADO"))
                .andExpect(jsonPath("$.historial.transportista.id").value(transportistaId))
                .andExpect(jsonPath("$.siguiente.pedidoId").value(segundo))
                .andExpect(jsonPath("$.avisoEnCaminoA").value("Almacén Godoy Cruz"));

        List<HistorialEstadoPedidoResponse> historial = pedidos.consultarHistorial(primero);
        assertThat(historial).extracting(HistorialEstadoPedidoResponse::estado).endsWith(
                EstadoPedido.EN_REPARTO, EstadoPedido.ENTREGADO);
        HistorialEstadoPedidoResponse entrega = historial.getLast();
        assertThat(entrega.fechaHora()).isNotNull();
        assertThat(entrega.usuarioId()).isEqualTo(distribuidor.getId());
        assertThat(entrega.transportista().id()).isEqualTo(transportistaId);
        assertThat(entrega.observacion()).isEqualTo("Recibió Marta.");

        // El correo sale después del commit, en otro hilo.
        verify(correo, timeout(5000)).enviar(anyString(), contains("N° 5202"), eq("correo/pedido-en-camino"), anyMap());

        // Entregar dos veces el mismo pedido se rechaza con el mensaje del State.
        mvc.perform(post("/api/distribucion/pedidos/" + primero + "/entrega")
                        .header(HttpHeaders.AUTHORIZATION, bearer(distribuidor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("ENTREGADO")));
        // Un pedido que no está en su ruta no se puede entregar ni consultar.
        mvc.perform(post("/api/distribucion/pedidos/" + sinPagar + "/entrega")
                        .header(HttpHeaders.AUTHORIZATION, bearer(distribuidor)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/distribucion/pedidos/" + sinPagar).header(HttpHeaders.AUTHORIZATION, bearer(distribuidor)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/distribucion/pedidos/" + segundo).header(HttpHeaders.AUTHORIZATION, bearer(distribuidor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedido.numeroPedido").value(5202));
    }

    @Test
    void soloElDistribuidorEntraAlPortal() throws Exception {
        mvc.perform(get("/api/distribucion/ruta")).andExpect(status().isUnauthorized());
        for (String alias : new String[] {"preventor", "administrativo", "jefe", "minorista"}) {
            mvc.perform(get("/api/distribucion/ruta").header(HttpHeaders.AUTHORIZATION, bearer(usuario(alias))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/distribucion/ruta").header(HttpHeaders.AUTHORIZATION, bearer(usuario("distribuidor"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(LocalDate.now().toString()));
    }

    // ---------------------------------------------------------------- auxiliares

    private String facturadoYPagado(long numero, PuntoDeVenta punto, Usuario administrativo) {
        return facturado(numero, punto, administrativo, EstadoFactura.PAGADA);
    }

    /** Pedido cargado y facturado. La factura se guarda a mano: acá no interesa el stock ni el PDF (E4-04). */
    private String facturado(long numero, PuntoDeVenta punto, Usuario administrativo, EstadoFactura estado) {
        Usuario preventor = usuario("preventor");
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(numero);
        pedido.setPreventor(preventores.findByUsuario_IdAndEliminadoFalse(preventor.getId()).orElseThrow());
        pedido.setPuntoDeVenta(punto);
        pedido.setTotal(1000);
        pedido.setSubtotal(1000);
        Pedido guardado = pedidos.registrarCarga(pedido, preventor.getId(), null);
        pedidos.avanzar(guardado.getId(), EventoPedido.FACTURAR, administrativo.getId(), null, null);
        Factura factura = new Factura();
        factura.setNumeroFactura(90000 + numero);
        factura.setFechaFactura(LocalDate.now());
        factura.setTotal(1000);
        factura.setEstado(estado);
        factura.setPedido(guardado);
        facturas.save(factura);
        return guardado.getId();
    }

    private List<PuntoDeVenta> puntosDelPreventor() {
        var preventor = preventores.findByUsuario_IdAndEliminadoFalse(usuario("preventor").getId()).orElseThrow();
        return puntosDeVenta.findByPreventor_IdAndEliminadoFalseOrderByNombreComercioAsc(preventor.getId());
    }

    private static PuntoDeVenta porNombre(List<PuntoDeVenta> puntos, String nombre) {
        return puntos.stream().filter(p -> p.getNombreComercio().equals(nombre)).findFirst().orElseThrow();
    }

    private Usuario usuario(String alias) {
        return usuarios.findByNombreUsuarioAndEliminadoFalse(alias + "@dulcemayor.com.ar").orElseThrow();
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwt.emitir(usuario);
    }
}

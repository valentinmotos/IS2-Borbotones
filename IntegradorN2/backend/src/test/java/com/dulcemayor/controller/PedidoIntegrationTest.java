package com.dulcemayor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dulcemayor.dto.HistorialEstadoPedidoResponse;
import com.dulcemayor.entity.Distribuidor;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.DistribuidorRepository;
import com.dulcemayor.repository.PedidoRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.PuntoDeVentaRepository;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.JwtService;
import com.dulcemayor.service.PedidoService;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedidoIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwt;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PreventorRepository preventores;
    @Autowired private PuntoDeVentaRepository puntosDeVenta;
    @Autowired private DistribuidorRepository distribuidores;
    @Autowired private PedidoRepository repositorio;
    @Autowired private PedidoService pedidos;

    @Test
    void persisteElCicloCompletoYExponeElHistorialSoloAGestion() throws Exception {
        Usuario preventor = usuario("preventor");
        Usuario administrativo = usuario("administrativo");
        Usuario distribuidor = usuario("distribuidor");
        Pedido pedido = cargar(4101, preventor);
        String id = pedido.getId();
        String transportistaId = crearTransportista(distribuidor);

        assertThatThrownBy(() -> pedidos.avanzar(id, EventoPedido.ENTREGAR, distribuidor.getId(), null, null))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("CARGADO", "ENTREGAR");
        assertThat(pedidos.consultarHistorial(id)).hasSize(1);
        assertThat(repositorio.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoPedido.CARGADO);

        pedidos.avanzar(id, EventoPedido.MARCAR_SIN_STOCK, administrativo.getId(), "  Faltan dos cajas.  ", null);
        pedidos.avanzar(id, EventoPedido.FACTURAR, administrativo.getId(), null, null);
        assertThatThrownBy(() -> pedidos.avanzar(id, EventoPedido.INICIAR_REPARTO, administrativo.getId(), null, null))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("transportista");
        pedidos.avanzar(id, EventoPedido.INICIAR_REPARTO, administrativo.getId(), null, transportistaId);
        pedidos.avanzar(id, EventoPedido.ENTREGAR, distribuidor.getId(), "Recibió el comercio.", null);
        assertThatThrownBy(() -> pedidos.avanzar(id, EventoPedido.ANULAR, administrativo.getId(), null, null))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("ENTREGADO", "ANULAR");

        var historial = pedidos.consultarHistorial(id);
        assertThat(historial).extracting(HistorialEstadoPedidoResponse::estado).containsExactly(
                EstadoPedido.CARGADO, EstadoPedido.SIN_STOCK, EstadoPedido.FACTURADO,
                EstadoPedido.EN_REPARTO, EstadoPedido.ENTREGADO);
        assertThat(historial).extracting(HistorialEstadoPedidoResponse::fechaHora).isSorted().doesNotHaveDuplicates();
        assertThat(historial.getFirst().fechaHora()).isEqualTo(pedido.getFechaHoraCarga());
        assertThat(historial.get(1).observacion()).isEqualTo("Faltan dos cajas.");
        assertThat(historial.get(3).transportista()).isEqualTo(historial.getLast().transportista());
        assertThat(repositorio.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoPedido.ENTREGADO);

        String url = "/api/pedidos/" + id + "/historial";
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(preventor))).andExpect(status().isForbidden());
        mvc.perform(get("/api/pedidos/inexistente/historial").header(HttpHeaders.AUTHORIZATION, bearer(administrativo)))
                .andExpect(status().isNotFound());
        mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(administrativo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(preventor.getId()))
                .andExpect(jsonPath("$[4].estado").value("ENTREGADO"))
                .andExpect(jsonPath("$[4].usuarioId").value(distribuidor.getId()))
                .andExpect(jsonPath("$[4].transportista.id").value(transportistaId));
    }

    @Test
    void unPedidoAnuladoEsTerminalYUnReintentoConcurrenteNoDuplicaElHistorial() throws Exception {
        String id = cargar(4102, usuario("preventor")).getId();
        String usuarioId = usuario("administrativo").getId();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var inicio = new CountDownLatch(1);
            var primero = executor.submit(() -> anularAlIniciar(inicio, id, usuarioId));
            var segundo = executor.submit(() -> anularAlIniciar(inicio, id, usuarioId));
            inicio.countDown();
            assertThat(primero.get(10, TimeUnit.SECONDS)).isNotEqualTo(segundo.get(10, TimeUnit.SECONDS));
        }
        assertThat(pedidos.consultarHistorial(id)).extracting(HistorialEstadoPedidoResponse::estado)
                .containsExactly(EstadoPedido.CARGADO, EstadoPedido.ANULADO);
        assertThatThrownBy(() -> pedidos.avanzar(id, EventoPedido.FACTURAR, usuarioId, null, null))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("ANULADO", "FACTURAR");
    }

    private boolean anularAlIniciar(CountDownLatch inicio, String id, String usuarioId) throws InterruptedException {
        inicio.await();
        try {
            pedidos.avanzar(id, EventoPedido.ANULAR, usuarioId, "Anulación de prueba.", null);
            return true;
        } catch (ErrorServiceException ex) {
            assertThat(ex).hasMessageContaining("ANULADO", "ANULAR");
            return false;
        }
    }

    private Pedido cargar(long numero, Usuario usuario) {
        var preventor = preventores.findByUsuario_IdAndEliminadoFalse(usuario.getId()).orElseThrow();
        var punto = puntosDeVenta.findByPreventor_IdAndEliminadoFalseOrderByNombreComercioAsc(preventor.getId())
                .getFirst();
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(numero);
        pedido.setPreventor(preventor);
        pedido.setPuntoDeVenta(punto);
        return pedidos.registrarCarga(pedido, usuario.getId(), null);
    }

    private String crearTransportista(Usuario usuario) {
        Distribuidor transportista = new Distribuidor();
        transportista.setNombre("Darío");
        transportista.setApellido("Distribuidor");
        transportista.setTipoDocumento(TipoDocumento.DNI);
        transportista.setNumeroDocumento("32104101");
        transportista.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        transportista.setLegajo("D-E4-01");
        transportista.setUsuario(usuario);
        return distribuidores.save(transportista).getId();
    }

    private Usuario usuario(String alias) {
        return usuarios.findByNombreUsuarioAndEliminadoFalse(alias + "@dulcemayor.com.ar").orElseThrow();
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwt.emitir(usuario);
    }
}

package com.dulcemayor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dulcemayor.entity.DetalleFactura;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.PedidoRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.PuntoDeVentaRepository;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.JwtService;
import com.dulcemayor.service.StockService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EstadisticasIntegrationTest {
    private static final String RUTA = "/api/estadisticas";

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwt;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PreventorRepository preventores;
    @Autowired private PuntoDeVentaRepository puntosDeVenta;
    @Autowired private ProductoRepository productos;
    @Autowired private PedidoRepository pedidos;
    @Autowired private FacturaRepository facturas;
    @Autowired private StockService stock;

    @Test
    void agrupaPedidosPorDiaYAcotaLasVentasAlPeriodoConStockActual() throws Exception {
        LocalDate dia = LocalDate.of(2024, 2, 29);
        Producto producto = productos.findByEliminadoFalse().getFirst();
        Pedido anterior = cargar(5401, dia.minusDays(29).atTime(23, 59), true);
        Pedido facturado = cargar(5402, dia.atStartOfDay(), true);
        Pedido pendiente = cargar(5403, dia.atTime(23, 59), false);
        Pedido posterior = cargar(5404, dia.plusDays(1).atStartOfDay(), true);
        factura(anterior, producto, dia.minusDays(29), 50, EstadoFactura.PAGADA);
        factura(facturado, producto, dia, 3, EstadoFactura.PENDIENTE_PAGO);
        factura(pendiente, producto, dia, 99, EstadoFactura.ANULADA);
        factura(posterior, producto, dia.plusDays(1), 8, EstadoFactura.PAGADA);

        mvc.perform(get(RUTA).param("anio", "2024").param("mes", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer("jefe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidos.length()").value(1))
                .andExpect(jsonPath("$.pedidos[0].dia").value("2024-02-29"))
                .andExpect(jsonPath("$.pedidos[0].cantidad").value(2))
                .andExpect(jsonPath("$.pedidos[0].facturados").value(1))
                .andExpect(jsonPath("$.productosMasVendidos[0].productoId").value(producto.getId()))
                .andExpect(jsonPath("$.productosMasVendidos[0].cantidad").value(3))
                .andExpect(jsonPath("$.stock[?(@.productoId == '%s')].cantidadActual".formatted(producto.getId()))
                        .value(stock.buscarStockPorProducto(producto.getId()).cantidadActual()));
        mvc.perform(get(RUTA).param("anio", "2024").header(HttpHeaders.AUTHORIZATION, bearer("jefe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidos.length()").value(3))
                .andExpect(jsonPath("$.productosMasVendidos[0].cantidad").value(61));
    }

    @Test
    void requiereJefaturaYValidaElPeriodo() throws Exception {
        mvc.perform(get(RUTA).param("anio", "2024")).andExpect(status().isUnauthorized());
        for (String rol : new String[] { "administrativo", "preventor", "distribuidor", "minorista" }) {
            mvc.perform(get(RUTA).param("anio", "2024").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get(RUTA).param("anio", "2024").param("mes", "13")
                        .header(HttpHeaders.AUTHORIZATION, bearer("jefe")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("mes"));
    }

    private Pedido cargar(long numero, LocalDateTime fecha, boolean facturado) {
        Usuario usuario = usuario("preventor");
        var preventor = preventores.findByUsuario_IdAndEliminadoFalse(usuario.getId()).orElseThrow();
        var punto = puntosDeVenta.findByPreventor_IdAndEliminadoFalseOrderByNombreComercioAsc(preventor.getId())
                .getFirst();
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(numero);
        pedido.setPreventor(preventor);
        pedido.setPuntoDeVenta(punto);
        pedido.registrarCarga(usuario, fecha, null);
        if (facturado) {
            pedido.avanzar(EventoPedido.FACTURAR, usuario("jefe"), fecha, null, null);
        }
        return pedidos.save(pedido);
    }

    private void factura(Pedido pedido, Producto producto, LocalDate fecha, int cantidad, EstadoFactura estado) {
        Factura factura = new Factura();
        factura.setPedido(pedido);
        factura.setNumeroFactura(pedido.getNumeroPedido());
        factura.setFechaFactura(fecha);
        factura.setEstado(estado);
        DetalleFactura detalle = new DetalleFactura();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        factura.agregarDetalle(detalle);
        facturas.save(factura);
    }

    private String bearer(String alias) {
        return "Bearer " + jwt.emitir(usuario(alias));
    }

    private Usuario usuario(String alias) {
        return usuarios.findByNombreUsuarioAndEliminadoFalse(alias + "@dulcemayor.com.ar").orElseThrow();
    }
}

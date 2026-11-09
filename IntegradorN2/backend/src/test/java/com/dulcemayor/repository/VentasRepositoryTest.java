package com.dulcemayor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.dulcemayor.entity.*;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.entity.enums.MonedaPago;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.RutaEstado;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.entity.enums.TipoPago;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * E0-07 · criterio: las tablas de ventas, pagos y rutas se crean con sus claves foráneas.
 * También verifica las composiciones (cascada y orphanRemoval) y las consultas de los repositorios.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VentasRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private PedidoRepository pedidos;
    @Autowired private DetallePedidoRepository detallesPedido;
    @Autowired private FacturaRepository facturas;
    @Autowired private DetalleFacturaRepository detallesFactura;
    @Autowired private PagoRepository pagos;
    @Autowired private CotizacionDolarRepository cotizaciones;
    @Autowired private RutaRepository rutas;
    @Autowired private ParadaRutaRepository paradas;

    private PuntoDeVenta puntoDeVenta;
    private Preventor preventor;
    private Producto producto;
    private int documentos;

    @BeforeEach
    void setUp() {
        Departamento departamento = new Departamento();
        departamento.setNombre("Capital");
        em.persist(departamento);
        Localidad localidad = new Localidad();
        localidad.setNombre("Mendoza");
        localidad.setCodigoPostal("5500");
        localidad.setDepartamento(departamento);
        em.persist(localidad);
        Direccion direccion = new Direccion();
        direccion.setCalle("San Martín");
        direccion.setNumeracion("1000");
        direccion.setLatitud(-32.89);
        direccion.setLongitud(-68.84);
        direccion.setLocalidad(localidad);
        em.persist(direccion);

        preventor = persona(new Preventor());
        preventor.setLegajo("P-001");
        Minorista minorista = persona(new Minorista());
        em.persist(preventor);
        em.persist(minorista);

        puntoDeVenta = new PuntoDeVenta();
        puntoDeVenta.setNombreComercio("Kiosco Centro");
        puntoDeVenta.setMinorista(minorista);
        puntoDeVenta.setDireccion(direccion);
        puntoDeVenta.setPreventor(preventor);
        em.persist(puntoDeVenta);

        Categoria categoria = new Categoria();
        categoria.setNombre("Alfajores");
        em.persist(categoria);
        producto = new Producto();
        producto.setCodigo("ALF-001");
        producto.setNombre("Alfajor");
        producto.setPresentacion("Caja x24");
        producto.setUnidadesPorBulto(24);
        producto.setCategoria(categoria);
        em.persist(producto);
    }

    @Test
    void lasTablasSeCreanConSusClavesForaneas() {
        @SuppressWarnings("unchecked")
        List<String> foraneas = em.getEntityManager().createNativeQuery("""
                select lower(k.table_name) || '.' || lower(k.column_name)
                from information_schema.table_constraints t
                join information_schema.key_column_usage k
                  on k.constraint_schema = t.constraint_schema and k.constraint_name = t.constraint_name
                where t.constraint_type = 'FOREIGN KEY'
                """).getResultList();

        assertThat(foraneas).contains(
                "pedidos.punto_de_venta_id", "pedidos.preventor_id",
                "detalles_pedido.pedido_id", "detalles_pedido.producto_id",
                "historial_estados_pedido.pedido_id", "historial_estados_pedido.usuario_id",
                "facturas.pedido_id",
                "detalles_factura.factura_id", "detalles_factura.producto_id",
                "pagos.factura_id", "pagos.forma_de_pago_id",
                "rutas.preventor_id", "rutas.distribuidor_id",
                "paradas_ruta.ruta_id", "paradas_ruta.punto_de_venta_id", "paradas_ruta.pedido_id");
    }

    @Test
    void elPedidoGuardaYBorraSuDetalleEnCascada() {
        Pedido pedido = pedido(1);
        pedido.agregarDetalle(detallePedido(2));
        pedido.agregarDetalle(detallePedido(3));
        pedidos.save(pedido);
        em.flush();
        em.clear();

        Pedido guardado = pedidos.findById(pedido.getId()).orElseThrow();
        assertThat(detallesPedido.findByPedido_IdAndEliminadoFalse(pedido.getId())).hasSize(2);

        guardado.getDetalles().remove(0);
        em.flush();
        assertThat(detallesPedido.findByPedido_IdAndEliminadoFalse(pedido.getId())).hasSize(1);
    }

    @Test
    void facturaConDetallePagoYConsultaPorMinorista() {
        Pedido pedido = pedidos.save(pedido(10));
        Factura factura = new Factura();
        factura.setNumeroFactura(100);
        factura.setFechaFactura(LocalDate.of(2026, 7, 1));
        factura.setTotal(30000);
        factura.setEstado(EstadoFactura.PENDIENTE_PAGO);
        factura.setPedido(pedido);
        DetalleFactura detalle = new DetalleFactura();
        detalle.setProducto(producto);
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(15000);
        detalle.setSubtotal(30000);
        factura.agregarDetalle(detalle);
        facturas.save(factura);

        FormaDePago transferencia = new FormaDePago();
        transferencia.setTipoPago(TipoPago.TRANSFERENCIA);
        transferencia.setObservacion("dulce.mayor.alias");
        em.persist(transferencia);
        Pago pago = new Pago();
        pago.setFactura(factura);
        pago.setFormaDePago(transferencia);
        pago.setMonto(30000);
        pago.setMoneda(MonedaPago.ARS);
        pago.setFechaPago(LocalDate.of(2026, 7, 2));
        pagos.save(pago);
        em.flush();
        em.clear();

        assertThat(detallesFactura.findAll()).hasSize(1);
        assertThat(facturas.findByPedido_IdAndEliminadoFalse(pedido.getId())).isPresent();
        assertThat(facturas.findByPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse(
                puntoDeVenta.getMinorista().getId())).extracting(Factura::getNumeroFactura).containsExactly(100L);
        assertThat(facturas.findByEstadoAndEliminadoFalse(EstadoFactura.PAGADA)).isEmpty();
        assertThat(pagos.findByFactura_IdAndEliminadoFalse(factura.getId()))
                .singleElement().extracting(Pago::getCotizacionAplicada).isNull();
    }

    @Test
    void cotizacionVigenteEsLaDeFechaMasReciente() {
        cotizacion(LocalDate.of(2026, 7, 1), 1200);
        cotizacion(LocalDate.of(2026, 7, 3), 1250);
        cotizacion(LocalDate.of(2026, 7, 2), 1220);
        em.flush();

        assertThat(cotizaciones.findFirstByOrderByFechaDesc())
                .get().extracting(CotizacionDolar::getValorVenta).isEqualTo(1250.0);
        assertThat(cotizaciones.findByFecha(LocalDate.of(2026, 7, 2))).isPresent();
    }

    @Test
    void laRutaGuardaSusParadasEnOrden() {
        Pedido pedido = pedidos.save(pedido(20));
        Ruta ruta = new Ruta();
        ruta.setFecha(LocalDate.of(2026, 7, 5));
        ruta.setEstado(RutaEstado.PENDIENTE);
        ruta.setPreventor(preventor);
        ruta.agregarParada(parada(2, null));
        ruta.agregarParada(parada(1, pedido));
        rutas.save(ruta);
        em.flush();
        em.clear();

        Ruta guardada = rutas.findByFechaAndEliminadoFalse(LocalDate.of(2026, 7, 5)).get(0);
        assertThat(guardada.getParadas()).extracting(ParadaRuta::getOrden).containsExactly(1, 2);
        assertThat(rutas.findByPreventor_IdAndEliminadoFalse(preventor.getId())).hasSize(1);
        assertThat(paradas.findAll()).hasSize(2);
    }

    @Test
    void historialRegistraEstadoYUsuario() {
        Pedido pedido = pedidos.save(pedido(30));
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("preventor@dulcemayor.com");
        usuario.setClave("hash");
        usuario.setRol(RolUsuario.PREVENTOR);
        em.persist(usuario);
        HistorialEstadoPedido historial = new HistorialEstadoPedido();
        historial.setPedido(pedido);
        historial.setEstado(EstadoPedido.CARGADO);
        historial.setFechaHora(LocalDateTime.of(2026, 7, 1, 10, 0));
        historial.setUsuario(usuario);
        em.persist(historial);
        em.flush();

        assertThat(pedidos.findByEstadoAndEliminadoFalse(EstadoPedido.CARGADO)).contains(pedido);
        assertThat(pedidos.findByPuntoDeVenta_IdAndEliminadoFalse(puntoDeVenta.getId())).contains(pedido);
    }

    // ---- Datos de prueba ----

    private <P extends Persona> P persona(P persona) {
        persona.setNombre("Ana");
        persona.setApellido("Pérez");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setTipoDocumento(TipoDocumento.DNI);
        persona.setNumeroDocumento(String.valueOf(30000000 + documentos++));
        return persona;
    }

    private Pedido pedido(long numero) {
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(numero);
        pedido.setFechaHoraCarga(LocalDateTime.of(2026, 7, 1, 9, 0));
        pedido.setSubtotal(30000);
        pedido.setTotal(30000);
        pedido.setEstado(EstadoPedido.CARGADO);
        pedido.setPuntoDeVenta(puntoDeVenta);
        pedido.setPreventor(preventor);
        return pedido;
    }

    private DetallePedido detallePedido(int cantidad) {
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(15000);
        detalle.setSubtotal(15000.0 * cantidad);
        return detalle;
    }

    private void cotizacion(LocalDate fecha, double valorVenta) {
        CotizacionDolar cotizacion = new CotizacionDolar();
        cotizacion.setFecha(fecha.atStartOfDay());
        cotizacion.setValorVenta(valorVenta);
        em.persist(cotizacion);
    }

    private ParadaRuta parada(int orden, Pedido pedido) {
        ParadaRuta parada = new ParadaRuta();
        parada.setPuntoDeVenta(puntoDeVenta);
        parada.setPedido(pedido);
        parada.setOrden(orden);
        return parada;
    }
}

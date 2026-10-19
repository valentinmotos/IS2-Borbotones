package com.zero.ecommerce.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.zero.ecommerce.entities.enums.EstadoFactura;

/**
 * Verifica que el mapeo funcione sobre SQLite: ids UUID, cascada de los detalles y fechas.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MapeoEntidadesTests {

    private final TestEntityManager em;

    MapeoEntidadesTests(@Autowired TestEntityManager em) {
        this.em = em;
    }

    @Test
    void compraAProveedorConDetalleYMovimientoDeStock() {
        Producto producto = new Producto();
        producto.setCodigo("REM-M");
        producto.setNombre("Remera Zero");
        producto.setTalle("M");
        em.persist(producto);

        FacturaProveedor factura = new FacturaProveedor();
        factura.setNumeroFactura(1);
        factura.setFechaFactura(LocalDate.of(2026, 3, 1));
        factura.setEstado(EstadoFactura.SIN_DEFINIR);
        DetalleFactura detalle = new DetalleFactura();
        detalle.setCantidad(20);
        detalle.setSubtotal(20000);
        detalle.setProducto(producto);
        detalle.setFactura(factura);
        factura.getDetalles().add(detalle);
        em.persist(factura);

        Stock movimiento = new Stock();
        movimiento.setProducto(producto);
        movimiento.setDetalleFactura(detalle);
        movimiento.setCantidadActual(detalle.getCantidad() * factura.getSignoStock());
        movimiento.setFecha(LocalDateTime.of(2026, 3, 2, 10, 30));
        em.persist(movimiento);

        VigenciaPrecio vigencia = new VigenciaPrecio();
        vigencia.setProducto(producto);
        vigencia.setPrecio(15000);
        vigencia.setFechaDesde(LocalDate.of(2026, 3, 1));
        em.persist(vigencia);

        em.flush();
        em.clear();

        Factura leida = em.find(Factura.class, factura.getId());
        assertThat(leida).isInstanceOf(FacturaProveedor.class);
        assertThat(leida.getId()).hasSize(36);
        assertThat(leida.isEliminado()).isFalse();
        assertThat(leida.getFechaFactura()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(leida.getDetalles()).hasSize(1);
        assertThat(leida.getDetalles().get(0).getProducto().getCodigo()).isEqualTo("REM-M");

        Stock stockLeido = em.find(Stock.class, movimiento.getId());
        assertThat(stockLeido.getCantidadActual()).isEqualTo(20);
        assertThat(stockLeido.getFecha()).isEqualTo(LocalDateTime.of(2026, 3, 2, 10, 30));

        assertThat(em.find(VigenciaPrecio.class, vigencia.getId()).getFechaHasta()).isNull();
    }

    @Test
    void signoDeStockPorTipoDeFactura() {
        assertThat(new FacturaProveedor().getSignoStock()).isEqualTo(1);
        assertThat(new FacturaCliente().getSignoStock()).isEqualTo(-1);
    }
}

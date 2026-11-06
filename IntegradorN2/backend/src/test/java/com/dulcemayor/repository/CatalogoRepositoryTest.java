package com.dulcemayor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.dulcemayor.entity.*;
import com.dulcemayor.entity.enums.TipoImagen;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogoRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ProductoRepository productos;
    @Autowired private VigenciaPrecioRepository precios;
    @Autowired private PromocionRepository promociones;

    @Test
    void recuperaLaImagenYLosBytesDelProducto() {
        Imagen imagen = new Imagen();
        imagen.setNombre("alfajor.png");
        imagen.setMime("image/png");
        imagen.setTipoImagen(TipoImagen.PRODUCTO);
        imagen.setContenido(new byte[] {0, 1, 2, -1});
        em.persist(imagen);
        Producto producto = producto("ALF-001");
        producto.setImagen(imagen);
        String id = producto.getId();
        em.flush();
        em.clear();

        assertThat(productos.findById(id).orElseThrow().getImagen().getContenido())
                .containsExactly(0, 1, 2, -1);
    }

    @Test
    void catalogoSoloIncluyeProductosActivosConStockActivoPositivo() {
        Producto disponible = producto("DISPONIBLE");
        stock(disponible, 10);
        stock(producto("SIN-STOCK"), 0);
        Producto eliminado = producto("ELIMINADO");
        eliminado.setEliminado(true);
        stock(eliminado, 10);
        Stock baja = stock(producto("STOCK-BAJA"), 10);
        baja.setEliminado(true);
        producto("SIN-REGISTRO-STOCK");
        em.flush();

        assertThat(productos.findProductosCatalogo()).containsExactly(disponible);
        disponible.setEnOferta(true);
        eliminado.setEnOferta(true);
        assertThat(productos.findByEliminadoFalseAndEnOfertaTrue()).containsExactly(disponible);
    }

    @Test
    void precioVigenteEsElQueNoTieneCierreYElHistorialExcluyeBajas() {
        Producto producto = producto("PRECIO");
        VigenciaPrecio anterior = precio(producto, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        VigenciaPrecio vigente = precio(producto, LocalDate.of(2026, 4, 1), null);
        VigenciaPrecio baja = precio(producto, LocalDate.of(2026, 5, 1), null);
        baja.setEliminado(true);
        precio(producto("OTRO"), LocalDate.of(2026, 6, 1), null);
        em.flush();

        assertThat(precios.findFirstByProducto_IdAndFechaHastaIsNullAndEliminadoFalseOrderByFechaDesdeDesc(producto.getId()))
                .contains(vigente);
        assertThat(precios.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeDesc(producto.getId()))
                .containsExactly(vigente, anterior);
        assertThat(precios.findFirstByProducto_IdAndFechaHastaIsNullAndEliminadoFalseOrderByFechaDesdeDesc("inexistente"))
                .isEmpty();
    }

    @Test
    void promocionRespetaFechasInclusivasCodigoYBajaLogica() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);
        Promocion promocion = new Promocion();
        promocion.setCodigo("VERANO");
        promocion.setDescripcion("Descuento por volumen");
        promocion.setPorcentajeDescuento(10);
        promocion.setCantidadMinima(5);
        promocion.setFechaDesde(desde);
        promocion.setFechaHasta(hasta);
        em.persistAndFlush(promocion);

        assertThat(promociones.findByCodigoIgnoreCaseAndEliminadoFalseAndFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(
                "verano", desde, desde)).contains(promocion);
        assertThat(promociones.findByCodigoIgnoreCaseAndEliminadoFalseAndFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(
                "VERANO", hasta, hasta)).contains(promocion);
        assertThat(promociones.findByCodigoIgnoreCaseAndEliminadoFalseAndFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(
                "VERANO", hasta.plusDays(1), hasta.plusDays(1))).isEmpty();
        promocion.setEliminado(true);
        assertThat(promociones.findByCodigoIgnoreCaseAndEliminadoFalseAndFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(
                "VERANO", desde, desde)).isEmpty();
    }

    private Producto producto(String codigo) {
        Categoria categoria = new Categoria();
        categoria.setNombre("Golosinas " + codigo);
        em.persist(categoria);
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre("Alfajor");
        producto.setPresentacion("Caja x12");
        producto.setUnidadesPorBulto(12);
        producto.setCategoria(categoria);
        return em.persist(producto);
    }

    private Stock stock(Producto producto, int cantidad) {
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setCantidadActual(cantidad);
        return em.persist(stock);
    }

    private VigenciaPrecio precio(Producto producto, LocalDate desde, LocalDate hasta) {
        VigenciaPrecio precio = new VigenciaPrecio();
        precio.setProducto(producto);
        precio.setFechaDesde(desde);
        precio.setFechaHasta(hasta);
        precio.setPrecio(15000);
        return em.persist(precio);
    }
}

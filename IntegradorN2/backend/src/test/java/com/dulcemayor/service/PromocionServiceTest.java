package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.PromocionRequest;
import com.dulcemayor.entity.Categoria;
import com.dulcemayor.entity.DetallePedido;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.CategoriaRepository;
import com.dulcemayor.repository.ProductoRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PromocionServiceTest {

    @Autowired
    private PromocionService promociones;

    @Autowired
    private CategoriaRepository categorias;

    @Autowired
    private ProductoRepository productos;

    @Test
    void promocionPorVolumenCalculaDescuentoYTotalDelPedido() {
        promociones.crear(request("volumen10", 10, null, 10));
        Producto alfajor = producto("ALFAJOR");
        Producto caramelo = producto("CARAMELO");
        Pedido pedido = pedido(30000, linea(alfajor, 6, 18000), linea(caramelo, 4, 12000));

        double descuento = promociones.aplicarPromocion(pedido, " VOLUMEN10 ");

        assertThat(descuento).isEqualTo(3000);
        assertThat(pedido.getCodigoPromocion()).isEqualTo("VOLUMEN10");
        assertThat(pedido.getSubtotal()).isEqualTo(30000);
        assertThat(pedido.getDescuento()).isEqualTo(3000);
        assertThat(pedido.getTotal()).isEqualTo(27000);
    }

    @Test
    void promocionPorProductoSoloDescuentaLaLineaDestacada() {
        Producto alfajor = producto("ALFAJOR-PROMO");
        Producto caramelo = producto("CARAMELO-NORMAL");
        promociones.crear(request("ALFAJOR20", null, alfajor.getId(), 20));
        Pedido pedido = pedido(30000, linea(alfajor, 5, 20000), linea(caramelo, 5, 10000));

        double descuento = promociones.aplicarPromocion(pedido, "alfajor20");

        assertThat(descuento).isEqualTo(4000);
        assertThat(pedido.getDescuento()).isEqualTo(4000);
        assertThat(pedido.getTotal()).isEqualTo(26000);
    }

    @Test
    void rechazaCodigoVencidoInexistenteYCantidadMinimaNoAlcanzada() {
        promociones.crear(new PromocionRequest("VENCIDA", "Promoción vencida", 10.0,
                2, null, LocalDate.now().minusDays(3), LocalDate.now().minusDays(1)));
        promociones.crear(request("VOLUMEN10", 10, null, 10));
        Producto alfajor = producto("ALFAJOR-MINIMO");
        Pedido pedido = pedido(18000, linea(alfajor, 6, 18000));

        assertThatThrownBy(() -> promociones.aplicarPromocion(pedido, "VENCIDA"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("no existe o está vencido");
        assertThatThrownBy(() -> promociones.aplicarPromocion(pedido, "INEXISTENTE"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("no existe o está vencido");
        assertThatThrownBy(() -> promociones.aplicarPromocion(pedido, "VOLUMEN10"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("cantidad mínima");
    }

    @Test
    void rechazaLaPromocionDeProductoSiNoEstaEnElPedido() {
        Producto alfajor = producto("ALFAJOR-AUSENTE");
        Producto caramelo = producto("CARAMELO-PRESENTE");
        promociones.crear(request("SOLOALFAJOR", null, alfajor.getId(), 15));
        Pedido pedido = pedido(10000, linea(caramelo, 5, 10000));

        assertThatThrownBy(() -> promociones.aplicarPromocion(pedido, "SOLOALFAJOR"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("no contiene el producto");
    }

    @Test
    void altaNormalizaCodigoYRechazaPromocionSinModalidadOConModalidadesCombinadas() {
        var creada = promociones.crear(request("  VERANO  ", 1, null, 12));

        assertThat(creada.codigo()).isEqualTo("VERANO");
        assertThatThrownBy(() -> promociones.crear(request("SIN-MODALIDAD", null, null, 10)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("por volumen o por producto");
        assertThatThrownBy(() -> promociones.crear(request("AMBAS", 5, "producto", 10)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("por volumen o por producto");
    }

    private PromocionRequest request(String codigo, Integer cantidadMinima, String idProducto,
            double porcentaje) {
        return new PromocionRequest(codigo, "Descuento de prueba", porcentaje, cantidadMinima, idProducto,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10));
    }

    private Producto producto(String codigo) {
        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría " + codigo);
        categorias.save(categoria);
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre("Producto " + codigo);
        producto.setPresentacion("Caja x 10");
        producto.setUnidadesPorBulto(10);
        producto.setCategoria(categoria);
        return productos.save(producto);
    }

    private Pedido pedido(double subtotal, DetallePedido... detalles) {
        Pedido pedido = new Pedido();
        pedido.setSubtotal(subtotal);
        for (DetallePedido detalle : detalles) pedido.agregarDetalle(detalle);
        return pedido;
    }

    private DetallePedido linea(Producto producto, int cantidad, double subtotal) {
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(subtotal / cantidad);
        detalle.setSubtotal(subtotal);
        return detalle;
    }
}

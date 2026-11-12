package com.dulcemayor.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.PedidoRepository;
import com.dulcemayor.repository.SucursalRepository;
import com.dulcemayor.repository.VigenciaPrecioRepository;
import com.dulcemayor.service.CatalogoService;
import com.dulcemayor.service.EstadisticasService;

/**
 * E6-04: con la base vacía y {@code app.seeder.demo=true} la aplicación arranca con datos para todas las
 * pantallas: ~20 productos con imagen e historial de precios, 4 sucursales con foto, varios preventores,
 * pedidos en todos los estados y ventas de varios meses.
 */
@SpringBootTest(properties = "app.seeder.demo=true")
@ActiveProfiles("test")
@Transactional
class DatosDemostracionIntegrationTest {

    @Autowired private ProductoRepository productos;
    @Autowired private VigenciaPrecioRepository vigencias;
    @Autowired private SucursalRepository sucursales;
    @Autowired private PreventorRepository preventores;
    @Autowired private PedidoRepository pedidos;
    @Autowired private FacturaRepository facturas;
    @Autowired private CatalogoService catalogo;
    @Autowired private EstadisticasService estadisticas;

    @Test
    void catalogoConImagenesEHistorialDePrecios() {
        var activos = productos.findByEliminadoFalse();
        assertThat(activos).hasSizeGreaterThanOrEqualTo(20).allMatch(p -> p.getImagen() != null);
        assertThat(vigencias.findAll()).hasSize(activos.size() * 3);
        // El chupetín sigue sin stock (lo usa la demo del chatbot) y no aparece en el catálogo público
        assertThat(catalogo.buscar(null, null)).hasSizeGreaterThanOrEqualTo(15)
                .noneMatch(p -> p.codigo().equals("CAR-001"));
    }

    @Test
    void empresaYPersonal() {
        assertThat(sucursales.findAll()).hasSize(4).allMatch(s -> s.getImagen() != null);
        assertThat(preventores.findByEliminadoFalse()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void pedidosEnTodosLosEstadosYVentasDeVariosMeses() {
        var estados = pedidos.findByEliminadoFalse().stream().map(Pedido::getEstado)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(EstadoPedido.class)));
        assertThat(estados).containsExactlyInAnyOrder(EstadoPedido.values());

        var meses = facturas.findByEliminadoFalse().stream().map(f -> YearMonth.from(f.getFechaFactura()))
                .collect(Collectors.toSet());
        assertThat(meses).hasSizeGreaterThanOrEqualTo(6);

        // HU-01: el panel del mes pasado tiene pedidos por preventor y productos más vendidos
        LocalDate mesPasado = LocalDate.now().minusMonths(1);
        var panel = estadisticas.consultar(mesPasado.getYear(), mesPasado.getMonthValue());
        assertThat(panel.pedidos()).isNotEmpty();
        assertThat(panel.pedidos().stream().map(p -> p.preventorId()).distinct()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(panel.productosMasVendidos()).isNotEmpty();
        assertThat(panel.stock()).anyMatch(s -> s.cantidadActual() > 0);
    }
}

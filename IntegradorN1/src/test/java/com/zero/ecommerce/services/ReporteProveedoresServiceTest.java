package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.PrecioProveedorDTO;
import com.zero.ecommerce.dto.ProductoProveedorDTO;
import com.zero.ecommerce.dto.ReporteProveedoresDTO;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.enums.EstadoFactura;

@ExtendWith(MockitoExtension.class)
class ReporteProveedoresServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 1);

    @Mock
    private FacturaProveedorService facturaProveedorService;
    @InjectMocks
    private ReporteProveedoresService service;

    private Producto remera;
    private Producto gorra;
    private Proveedor cuyo;
    private Proveedor delPlata;
    private Proveedor andina;

    @BeforeEach
    void datos() {
        Categoria hombres = categoria("cat-hombres", "Hombres");
        remera = producto("remera", "REM-DRY-M", "Remera Dry Fit", "M", hombres);
        gorra = producto("gorra", "GOR-TRN-U", "Gorra Training", "Único", categoria("cat-acc", "Accesorios"));
        cuyo = proveedor("cuyo", "Deportiva Cuyo");
        delPlata = proveedor("plata", "Textiles del Plata");
        andina = proveedor("andina", "Fitness Andina");
    }

    @Test
    void recomiendaElProveedorMasBaratoYCalculaLaDiferenciaDeLosOtros() {
        recibidas(compra(1, cuyo, HOY.minusDays(20), remera, 5000),
                compra(2, delPlata, HOY.minusDays(15), remera, 4000),
                compra(3, andina, HOY.minusDays(10), remera, 6000));

        ProductoProveedorDTO fila = service.generar().productos().get(0);

        assertThat(fila.recomendado().razonSocial()).isEqualTo("Textiles del Plata");
        assertThat(fila.recomendado().precioCosto()).isEqualTo(4000);
        assertThat(fila.recomendado().fechaCompra()).isEqualTo(HOY.minusDays(15));
        assertThat(fila.proveedores()).extracting(PrecioProveedorDTO::razonSocial)
                .containsExactly("Textiles del Plata", "Deportiva Cuyo", "Fitness Andina");
        assertThat(fila.proveedores()).extracting(PrecioProveedorDTO::recomendado).containsExactly(true, false, false);
        assertThat(fila.proveedores().get(1).diferenciaPorcentual()).isCloseTo(25, within(0.001));
        assertThat(fila.proveedores().get(2).diferenciaPorcentual()).isCloseTo(50, within(0.001));
    }

    @Test
    void comparaElUltimoPrecioDeCadaProveedorYNoUnoViejo() {
        // Cuyo fue el más barato hace un mes, pero su última compra ya es más cara que la de Del Plata.
        recibidas(compra(1, cuyo, HOY.minusDays(30), remera, 3000),
                compra(2, delPlata, HOY.minusDays(20), remera, 4000),
                compra(3, cuyo, HOY.minusDays(5), remera, 4500));

        ProductoProveedorDTO fila = service.generar().productos().get(0);

        assertThat(fila.recomendado().razonSocial()).isEqualTo("Textiles del Plata");
        assertThat(fila.proveedores()).hasSize(2);
        assertThat(fila.proveedores().get(1).precioCosto()).isEqualTo(4500);
        assertThat(fila.proveedores().get(1).numeroCompra()).isEqualTo(3);
    }

    @Test
    void buscarProveedorMasEconomicoDevuelveElDeMenorCosto() {
        recibidas(compra(1, cuyo, HOY.minusDays(20), remera, 5000),
                compra(2, delPlata, HOY.minusDays(15), remera, 4000));

        assertThat(service.buscarProveedorMasEconomico("remera")).contains(delPlata);
    }

    @Test
    void sinComprasRecibidasNoHayProveedorMasEconomico() {
        recibidas(compra(1, cuyo, HOY.minusDays(20), remera, 5000));

        assertThat(service.buscarProveedorMasEconomico("gorra")).isEmpty();
        assertThat(service.buscarProveedorMasEconomico("")).isEmpty();
    }

    @Test
    void unProveedorDadoDeBajaNoSeRecomienda() {
        delPlata.setEliminado(true);
        recibidas(compra(1, cuyo, HOY.minusDays(20), remera, 5000),
                compra(2, delPlata, HOY.minusDays(15), remera, 4000));

        assertThat(service.buscarProveedorMasEconomico("remera")).contains(cuyo);
    }

    @Test
    void filtraPorCategoriaYTextoYCalculaLosTotales() {
        recibidas(compra(1, cuyo, HOY.minusDays(20), remera, 5000),
                compra(2, delPlata, HOY.minusDays(15), remera, 4000),
                compra(3, andina, HOY.minusDays(10), gorra, 2000));

        ReporteProveedoresDTO completo = service.generar();
        assertThat(completo.cantidadProductos()).isEqualTo(2);
        assertThat(completo.cantidadProveedores()).isEqualTo(3);
        assertThat(completo.cantidadConVariosProveedores()).isEqualTo(1);

        assertThat(service.generar(null, "cat-acc").productos()).extracting(ProductoProveedorDTO::codigo)
                .containsExactly("GOR-TRN-U");
        assertThat(service.generar("REMERA dry", null).productos()).extracting(ProductoProveedorDTO::codigo)
                .containsExactly("REM-DRY-M");
    }

    private void recibidas(FacturaProveedor... compras) {
        when(facturaProveedorService.listarFacturaPorEstado(EstadoFactura.PAGADA)).thenReturn(List.of(compras));
    }

    private FacturaProveedor compra(long numero, Proveedor proveedor, LocalDate fecha, Producto producto,
            double precioCosto) {
        FacturaProveedor compra = new FacturaProveedor();
        compra.setId("compra-" + numero);
        compra.setNumeroFactura(numero);
        compra.setProveedor(proveedor);
        compra.setFechaFactura(fecha);
        compra.setEstado(EstadoFactura.PAGADA);
        compra.agregarDetalle(producto, 10, precioCosto);
        return compra;
    }

    private Proveedor proveedor(String id, String razonSocial) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(id);
        proveedor.setRazonSocial(razonSocial);
        return proveedor;
    }

    private Categoria categoria(String id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }

    private Producto producto(String id, String codigo, String nombre, String talle, Categoria categoria) {
        SubCategoria subCategoria = new SubCategoria();
        subCategoria.setNombre("General");
        subCategoria.setCategoria(categoria);
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setTalle(talle);
        producto.setSubCategoria(subCategoria);
        return producto;
    }
}

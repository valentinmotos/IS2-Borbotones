package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.ProductoStockDTO;
import com.zero.ecommerce.dto.ReporteStockDTO;
import com.zero.ecommerce.dto.ReposicionStockDTO;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.entities.enums.EstadoStock;
import com.zero.ecommerce.entities.enums.TipoTelefono;

@ExtendWith(MockitoExtension.class)
class ReposicionStockServiceTest {

    @Mock
    private ReporteStockService reporteStockService;
    @Mock
    private ReporteProveedoresService reporteProveedoresService;
    @Mock
    private ProveedorService proveedorService;
    private ReposicionStockService service;

    @BeforeEach
    void setUp() {
        service = new ReposicionStockService(reporteStockService, reporteProveedoresService, proveedorService);
    }

    @Test
    void calculaUnidadesRedondeandoLaMitadHaciaArribaYRecomiendaElMasEconomico() {
        ProductoStockDTO producto = producto(3, 21, EstadoStock.MALO);
        Proveedor proveedor = proveedor();
        when(reporteProveedoresService.buscarProveedorMasEconomico("producto-1"))
                .thenReturn(Optional.of(proveedor));

        ReposicionStockDTO reposicion = service.listar(List.of(producto)).get("producto-1");

        assertThat(reposicion.unidades()).isEqualTo(8);
        assertThat(reposicion.proveedorId()).isEqualTo("proveedor-1");
        assertThat(reposicion.proveedorNombre()).isEqualTo("Proveedor Económico");
    }

    @Test
    void generaUrlConCelularYMensajeCodificado() throws Exception {
        ProductoStockDTO producto = producto(3, 20, EstadoStock.MALO);
        Proveedor proveedor = proveedor();
        when(reporteStockService.generar()).thenReturn(
                new ReporteStockDTO("sede", "Zero", 3, 0, 0, 1, List.of(producto)));
        when(reporteProveedoresService.buscarProveedorMasEconomico("producto-1"))
                .thenReturn(Optional.of(proveedor));

        String url = service.generarUrlWhatsApp("producto-1", null);
        String mensaje = URLDecoder.decode(URI.create(url).getRawQuery().substring("text=".length()),
                StandardCharsets.UTF_8);

        assertThat(url).startsWith("https://wa.me/5492614123456?text=");
        assertThat(mensaje).contains("Hola, Proveedor Económico.", "Producto: Calza Zero Fit", "Código: CAL-FIT-M",
                "Talle: M", "Cantidad: 7 unidades", "Saludos,\nZero");
    }

    @Test
    void rechazaLaReposicionDeUnProductoQueYaNoEstaEnEstadoMalo() throws Exception {
        ProductoStockDTO producto = producto(10, 20, EstadoStock.REGULAR);
        when(reporteStockService.generar()).thenReturn(
                new ReporteStockDTO("sede", "Zero", 10, 0, 1, 0, List.of(producto)));

        assertThatThrownBy(() -> service.generarUrlWhatsApp("producto-1", null))
                .hasMessage("Solo se puede pedir reposición para productos con stock Malo.");
    }

    private ProductoStockDTO producto(int actual, int referencia, EstadoStock estado) {
        return new ProductoStockDTO("producto-1", "CAL-FIT-M", "Calza Zero Fit", "M", "cat-1", "Mujeres",
                "Ropa", actual, referencia, 15, 15, estado);
    }

    private Proveedor proveedor() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId("proveedor-1");
        proveedor.setRazonSocial("Proveedor Económico");
        ContactoTelefonico celular = new ContactoTelefonico();
        celular.setTelefono("5492614123456");
        celular.setTipoTelefono(TipoTelefono.CELULAR);
        proveedor.setContactos(List.of(celular));
        return proveedor;
    }
}

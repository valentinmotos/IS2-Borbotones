package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dulcemayor.entity.DetalleFactura;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.Localidad;
import com.dulcemayor.entity.Mayorista;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.MayoristaRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FacturaPdfServiceTest {
    @Mock private FacturaRepository facturas;
    @Mock private MayoristaRepository mayoristas;
    private FacturaPdfService service;

    @BeforeEach
    void setUp() {
        service = new FacturaPdfService(facturas, mayoristas);
    }

    @Test
    void generaUnPdfConLosDatosDeLaFacturaYSusTotales() throws Exception {
        when(facturas.findByIdAndEliminadoFalse("factura-1")).thenReturn(Optional.of(factura()));
        Mayorista empresa = new Mayorista();
        empresa.setRazonSocial("Dulce Mayor S.A.");
        empresa.setCuit("30-71234567-8");
        when(mayoristas.findByEliminadoFalse()).thenReturn(List.of(empresa));

        byte[] pdf = service.generarFacturaPdf("factura-1");

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        String texto = texto(pdf);
        assertThat(texto).contains("Dulce Mayor S.A.", "30-71234567-8", "FACTURA", "00000012", "05/11/2026",
                "Pendiente de pago", "Kiosco Centro", "Inés Prueba", "San Martín 100, Mendoza", "987654",
                "Alfajor", "ALF-001", "$ 3.000,00", "PROMO10", "- $ 300,00", "$ 2.700,00");
    }

    @Test
    void elNombreDelArchivoLlevaElNumeroDeFactura() {
        when(facturas.findByIdAndEliminadoFalse("factura-1")).thenReturn(Optional.of(factura()));

        assertThat(service.nombreArchivo("factura-1")).isEqualTo("factura-00000012.pdf");
    }

    @Test
    void unaFacturaInexistenteDa404() {
        when(facturas.findByIdAndEliminadoFalse("nada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generarFacturaPdf("nada")).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void formateaLosImportesEnPesosArgentinos() {
        assertThat(FacturaPdfService.pesos(1234567.5)).isEqualTo("$ 1.234.567,50");
    }

    private static String texto(byte[] pdf) throws Exception {
        PdfReader lector = new PdfReader(pdf);
        try {
            StringBuilder texto = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                texto.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return texto.toString();
        } finally {
            lector.close();
        }
    }

    private static Factura factura() {
        Minorista minorista = new Minorista();
        minorista.setNombre("Inés");
        minorista.setApellido("Prueba");
        minorista.setTipoDocumento(TipoDocumento.DNI);
        minorista.setNumeroDocumento("32126802");
        Localidad localidad = new Localidad();
        localidad.setNombre("Mendoza");
        Direccion direccion = new Direccion();
        direccion.setCalle("San Martín");
        direccion.setNumeracion("100");
        direccion.setLocalidad(localidad);
        PuntoDeVenta punto = new PuntoDeVenta();
        punto.setNombreComercio("Kiosco Centro");
        punto.setMinorista(minorista);
        punto.setDireccion(direccion);
        Preventor preventor = new Preventor();
        preventor.setNombre("Pablo");
        preventor.setApellido("Preventor");

        Pedido pedido = new Pedido();
        Usuario usuario = new Usuario();
        pedido.registrarCarga(usuario, LocalDateTime.of(2026, 11, 4, 10, 30), null);
        pedido.setNumeroPedido(987654L);
        pedido.setPuntoDeVenta(punto);
        pedido.setPreventor(preventor);
        pedido.setSubtotal(3000);
        pedido.setDescuento(300);
        pedido.setTotal(2700);
        pedido.setCodigoPromocion("PROMO10");

        Producto producto = new Producto();
        producto.setCodigo("ALF-001");
        producto.setNombre("Alfajor");
        producto.setPresentacion("Caja x24");
        Factura factura = new Factura();
        factura.setNumeroFactura(12);
        factura.setFechaFactura(LocalDate.of(2026, 11, 5));
        factura.setEstado(EstadoFactura.PENDIENTE_PAGO);
        factura.setTotal(2700);
        factura.setPedido(pedido);
        DetalleFactura linea = new DetalleFactura();
        linea.setProducto(producto);
        linea.setCantidad(3);
        linea.setPrecioUnitario(1000);
        linea.setSubtotal(3000);
        factura.agregarDetalle(linea);
        return factura;
    }
}

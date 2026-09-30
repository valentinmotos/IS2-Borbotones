package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.ItemCorreoCompraDTO;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.entities.enums.TipoPago;

@ExtendWith(MockitoExtension.class)
class NotificacionCompraServiceTest {

    @Mock EmailService emailService;
    @Mock OrdenCompraService ordenCompraService;

    private NotificacionCompraService service;
    private OrdenCompra orden;

    @BeforeEach
    void preparar() {
        service = new NotificacionCompraService(emailService, ordenCompraService, "https://zero.example/");
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("cliente@zero.test");
        Cliente cliente = new Cliente();
        cliente.setNombre("Lucía");
        cliente.setUsuario(usuario);
        Producto producto = new Producto();
        producto.setNombre("Remera Dry Fit");
        producto.setTalle("M");

        orden = new OrdenCompra();
        orden.setId("orden-1");
        orden.setIdentificadorCompra("ORD-0001");
        orden.setFecha(LocalDate.of(2026, 9, 26));
        orden.setCliente(cliente);
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_PAGO);
        orden.crearDetalle(producto, 2, 10000);
    }

    @Test
    @SuppressWarnings("unchecked")
    void laConfirmacionLlevaLosDatosDeLaCompraYElLinkAlSeguimiento() {
        when(ordenCompraService.buscarFacturaDePedido("orden-1"))
                .thenReturn(Optional.of(factura(TipoPago.TRANSFERENCIA, "Transferencia")));

        service.enviarConfirmacion(orden);

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(emailService).enviar(eq("cliente@zero.test"), eq(NotificacionCompraService.ASUNTO_CONFIRMACION),
                eq("confirmacion-compra"), variables.capture());
        assertThat(variables.getValue())
                .containsEntry("identificador", "ORD-0001")
                .containsEntry("fecha", "26/09/2026")
                .containsEntry("total", "$20.000")
                .containsEntry("numeroFactura", "8")
                .containsEntry("formaDePago", "Transferencia")
                .containsEntry("urlSeguimiento", "https://zero.example/cliente/compras/orden-1");
        assertThat(variables.getValue()).doesNotContainKey("urlPago");
        assertThat((List<ItemCorreoCompraDTO>) variables.getValue().get("items"))
                .containsExactly(new ItemCorreoCompraDTO("Remera Dry Fit (talle M)", 2, "$10.000", "$20.000"));
        assertThat((String) variables.getValue().get("instruccionesPago")).contains("transferencia");
    }

    @Test
    @SuppressWarnings("unchecked")
    void conMercadoPagoPendienteAclaraQueEsSimulado() {
        when(ordenCompraService.buscarFacturaDePedido("orden-1"))
                .thenReturn(Optional.of(factura(TipoPago.BILLETERA_VIRTUAL, "Mercado Pago")));

        service.enviarConfirmacion(orden);

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(emailService).enviar(any(), any(), any(), variables.capture());
        assertThat(variables.getValue()).doesNotContainKey("urlPago");
        assertThat((String) variables.getValue().get("instruccionesPago"))
                .contains("Mercado Pago simulado", "confirmación administrativa");
    }

    @Test
    @SuppressWarnings("unchecked")
    void elCambioDeEstadoAvisaElNuevoEstadoConLinkAlSeguimiento() throws Exception {
        orden.registrarPago();
        when(ordenCompraService.listarEstadoPedido()).thenReturn(Map.of("PENDIENTE_ENVIO", "Pendiente de envío"));

        service.notificarCambioEstado(orden);

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(emailService).enviar(eq("cliente@zero.test"), eq(NotificacionCompraService.ASUNTO_CAMBIO_ESTADO),
                eq("cambio-estado"), variables.capture());
        assertThat(variables.getValue())
                .containsEntry("estado", "Pendiente de envío")
                .containsEntry("urlSeguimiento", "https://zero.example/cliente/compras/orden-1");
    }

    @Test
    void sinClienteConUsuarioNoEnviaNada() {
        orden.getCliente().setUsuario(null);

        service.enviarConfirmacion(orden);
        service.notificarCambioEstado(orden);

        verify(emailService, never()).enviar(any(), any(), any(), any());
    }

    private FacturaCliente factura(TipoPago tipoPago, String observacion) {
        FormaDePago formaDePago = new FormaDePago();
        formaDePago.setTipoPago(tipoPago);
        formaDePago.setObservacion(observacion);
        FacturaCliente factura = new FacturaCliente();
        factura.setNumeroFactura(8);
        factura.setFormaDePago(formaDePago);
        return factura;
    }
}

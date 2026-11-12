package com.dulcemayor.service;

import com.dulcemayor.dto.CheckoutPagoResponse;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.FormaDePago;
import com.dulcemayor.entity.OrdenPagoMercadoPago;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.TipoPago;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.FormaDePagoRepository;
import com.dulcemayor.repository.OrdenPagoMercadoPagoRepository;
import com.dulcemayor.security.MercadoPagoWebhookVerifier;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient.Config;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient.CreateOrderRequest;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient.Item;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient.Online;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient.Payer;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient.OrderResponse;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient.PaymentTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Flujo Checkout Pro vía Orders API: crea la URL y procesa los Webhooks confirmados con MP. */
@Service
public class CheckoutMercadoPagoService {
    private final FacturaRepository facturas;
    private final OrdenPagoMercadoPagoRepository ordenes;
    private final FormaDePagoRepository formas;
    private final PagoService pagos;
    private final MercadoPagoOrdersClient mercadoPago;
    private final String successUrl;
    private final String failureUrl;
    private final String pendingUrl;
    private final String webhookSecret;

    public CheckoutMercadoPagoService(FacturaRepository facturas, OrdenPagoMercadoPagoRepository ordenes,
            FormaDePagoRepository formas, PagoService pagos, MercadoPagoOrdersClient mercadoPago,
            @Value("${app.mercadopago.success-url:}") String successUrl,
            @Value("${app.mercadopago.failure-url:}") String failureUrl,
            @Value("${app.mercadopago.pending-url:}") String pendingUrl,
            @Value("${app.mercadopago.webhook-secret:}") String webhookSecret) {
        this.facturas = facturas;
        this.ordenes = ordenes;
        this.formas = formas;
        this.pagos = pagos;
        this.mercadoPago = mercadoPago;
        this.successUrl = trim(successUrl);
        this.failureUrl = trim(failureUrl);
        this.pendingUrl = trim(pendingUrl);
        this.webhookSecret = trim(webhookSecret);
    }

    /** Crea o reutiliza el checkout abierto de una factura que pertenece al minorista autenticado. */
    @Transactional
    public CheckoutPagoResponse crearCheckout(String facturaId, String usuarioId, String emailComprador) {
        Factura factura = facturas.buscarActivaParaActualizar(facturaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la factura."));
        validarPropietario(factura, usuarioId);
        if (factura.getEstado() != EstadoFactura.PENDIENTE_PAGO) {
            throw new ErrorServiceException("La factura no está pendiente de pago.", "facturaId");
        }
        var checkoutExistente = ordenes.findFirstByFactura_IdAndEstadoInAndEliminadoFalseOrderByFechaCreacionDesc(
                facturaId, java.util.List.of("created", "processing", "action_required"));
        if (checkoutExistente.isPresent()) return respuesta(checkoutExistente.get());

        BigDecimal saldo = pagos.saldoPendienteEnPesos(facturaId).setScale(2, RoundingMode.HALF_UP);
        if (saldo.signum() <= 0) throw new ErrorServiceException("La factura no tiene saldo pendiente.", "facturaId");

        formas.findByEliminadoFalse().stream().filter(f -> f.getTipoPago() == TipoPago.BILLETERA_VIRTUAL)
                .findFirst().orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay una forma de pago de billetera virtual activa."));

        String idempotencyKey = UUID.randomUUID().toString();
        String referencia = UUID.randomUUID().toString();
        Online online = (successUrl == null && failureUrl == null && pendingUrl == null) ? null
                : new Online(successUrl, failureUrl, pendingUrl, successUrl == null ? null : "approved");
        CreateOrderRequest request = new CreateOrderRequest("online", "manual", "automatic_async",
                saldo.toPlainString(), referencia,
                "Factura " + factura.getNumeroFactura() + " - Dulce Mayor",
                emailComprador == null || !emailComprador.contains("@") ? null : new Payer(emailComprador),
                java.util.List.of(new Item("Factura " + factura.getNumeroFactura(), saldo.toPlainString(), 1)),
                online == null ? null : new Config(online));

        OrderResponse creada = mercadoPago.crearOrden(request, idempotencyKey);
        if (creada == null || creada.id() == null || creada.id().isBlank()
                || creada.checkoutUrl() == null || creada.checkoutUrl().isBlank()) {
            throw new ErrorServiceException("Mercado Pago no devolvió una orden de checkout válida.");
        }
        if (creada.totalAmount() != null && creada.totalAmount().compareTo(saldo) != 0) {
            throw new ErrorServiceException("El importe devuelto por Mercado Pago no coincide con el saldo de la factura.");
        }

        OrdenPagoMercadoPago orden = new OrdenPagoMercadoPago();
        orden.setFactura(factura);
        orden.setClaveIdempotencia(idempotencyKey);
        orden.setOrdenExternaId(creada.id());
        orden.setReferenciaExterna(referencia);
        orden.setUrlCheckout(creada.checkoutUrl());
        orden.setEstado(creada.status() == null ? "created" : creada.status());
        orden.setDetalleEstado(creada.statusDetail());
        orden.setMonto(saldo);
        orden.setMontoPagado(BigDecimal.ZERO.setScale(2));
        orden.setFechaCreacion(LocalDateTime.now());
        return respuesta(ordenes.save(orden));
    }

    /** Procesa únicamente notificaciones firmadas y confirma el estado contra GET /v1/orders/{id}. */
    @Transactional
    public void procesarWebhook(String orderId, String firma, String requestId, String dataId) {
        if (webhookSecret == null) {
            throw new ErrorServiceException("Webhook de Mercado Pago no configurado.");
        }
        if (!MercadoPagoWebhookVerifier.esValida(firma, requestId, dataId, webhookSecret)) {
            throw new ErrorServiceException("La firma del webhook de Mercado Pago no es válida.");
        }
        if (orderId == null || orderId.isBlank() || !orderId.equals(dataId)) {
            throw new ErrorServiceException("El identificador de la orden del webhook no coincide.");
        }

        OrdenPagoMercadoPago local = ordenes.buscarPorOrdenExternaParaActualizar(orderId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de pago local."));
        if ("processed".equals(local.getEstado())) return; // Webhook repetido: operación idempotente.

        OrderResponse remota = mercadoPago.buscarOrden(orderId);
        if (remota == null || !orderId.equals(remota.id())
                || !local.getReferenciaExterna().equals(remota.externalReference())
                || (remota.type() != null && !"online".equals(remota.type()))
                || remota.totalAmount() == null || remota.totalAmount().compareTo(local.getMonto()) != 0) {
            throw new ErrorServiceException("La orden consultada no coincide con el checkout local.");
        }
        local.setDetalleEstado(remota.statusDetail());
        local.setMontoPagado(remota.totalPaidAmount() == null ? BigDecimal.ZERO : remota.totalPaidAmount());
        if ("processed".equals(remota.status())) {
            if (local.getMontoPagado().compareTo(local.getMonto()) != 0) {
                throw new ErrorServiceException("El importe acreditado por Mercado Pago no coincide con el total esperado.");
            }
            PaymentTransaction pagoAcreditado = remota.transactions() == null || remota.transactions().payments() == null
                    ? null : remota.transactions().payments().stream()
                            .filter(p -> "processed".equals(p.status()) && p.id() != null && !p.id().isBlank())
                            .findFirst().orElse(null);
            if (pagoAcreditado == null) {
                throw new ErrorServiceException("La orden procesada no contiene un pago acreditado.");
            }
            // Evita que un canal alternativo y un checkout simultáneo sobrepaguen la factura.
            local.setEstado("processing");
            ordenes.save(local);
            pagos.registrarMercadoPago(local.getFactura().getId(), local.getMonto(), pagoAcreditado.id());
            local.setEstado("processed");
            local.setPagoExternoId(pagoAcreditado.id());
        } else if (remota.status() != null && java.util.List.of(
                "created", "processing", "action_required", "canceled", "failed", "expired")
                .contains(remota.status())) {
            local.setEstado(remota.status());
        } else {
            throw new ErrorServiceException("Mercado Pago devolvió un estado de orden desconocido.");
        }
        ordenes.save(local);
    }

    @Transactional(readOnly = true)
    public CheckoutPagoResponse consultarCheckout(String facturaId, String usuarioId) {
        Factura factura = facturas.findById(facturaId).filter(f -> !f.isEliminado())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la factura."));
        validarPropietario(factura, usuarioId);
        return ordenes.findFirstByFactura_IdAndEstadoInAndEliminadoFalseOrderByFechaCreacionDesc(
                        facturaId, java.util.List.of("created", "processing", "action_required", "processed",
                                "canceled", "failed", "expired"))
                .map(CheckoutMercadoPagoService::respuesta)
                .orElseThrow(() -> new RecursoNoEncontradoException("La factura todavía no tiene un checkout."));
    }

    private static void validarPropietario(Factura factura, String usuarioId) {
        var punto = factura.getPedido().getPuntoDeVenta();
        var minorista = punto == null ? null : punto.getMinorista();
        var usuario = minorista == null ? null : minorista.getUsuario();
        if (usuario == null || !usuario.getId().equals(usuarioId)) {
            throw new RecursoNoEncontradoException("No se encontró la factura.");
        }
    }

    private static CheckoutPagoResponse respuesta(OrdenPagoMercadoPago orden) {
        return new CheckoutPagoResponse(orden.getFactura().getId(), orden.getOrdenExternaId(),
                orden.getUrlCheckout(), orden.getEstado(), orden.getMonto(), "ARS");
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.dulcemayor.service;

import com.dulcemayor.dto.CotizacionDolarResponse;
import com.dulcemayor.dto.PagoRequest;
import com.dulcemayor.dto.PagoResponse;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.FormaDePago;
import com.dulcemayor.entity.Pago;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.MonedaPago;
import com.dulcemayor.entity.enums.TipoPago;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.FormaDePagoRepository;
import com.dulcemayor.repository.PagoRepository;
import com.dulcemayor.repository.OrdenPagoMercadoPagoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro de pagos de facturas en ARS o USD, incluyendo la cotización aplicada. */
@Service
@Transactional(readOnly = true)
public class PagoService {
    private final PagoRepository pagos;
    private final FacturaRepository facturas;
    private final FormaDePagoRepository formas;
    private final CotizacionDolarService cotizaciones;
    private final OrdenPagoMercadoPagoRepository ordenesOnline;

    public PagoService(PagoRepository pagos, FacturaRepository facturas, FormaDePagoRepository formas,
            CotizacionDolarService cotizaciones, OrdenPagoMercadoPagoRepository ordenesOnline) {
        this.pagos = pagos;
        this.facturas = facturas;
        this.formas = formas;
        this.cotizaciones = cotizaciones;
        this.ordenesOnline = ordenesOnline;
    }

    @Transactional
    public PagoResponse registrar(PagoRequest request) {
        if (request == null) throw new ErrorServiceException("Indicá los datos del pago.");
        Factura factura = buscarFacturaBloqueada(request.facturaId());
        if (ordenesOnline.findFirstByFactura_IdAndEstadoInAndEliminadoFalseOrderByFechaCreacionDesc(
                factura.getId(), java.util.List.of("created", "processing", "action_required")).isPresent()) {
            throw new ErrorServiceException(
                    "La factura tiene un checkout de Mercado Pago pendiente. Esperá su resultado antes de registrar otro pago.",
                    "facturaId");
        }
        FormaDePago forma = formaActiva(request.formaDePagoId());
        return registrarEnFactura(factura, forma, request.monto(), request.moneda(), null,
                request.observacion());
    }

    /** Registra el pago acreditado por el webhook. No se llama a partir de la redirección del comprador. */
    @Transactional
    public PagoResponse registrarMercadoPago(String facturaId, BigDecimal monto, String referencia) {
        Factura factura = buscarFacturaBloqueada(facturaId);
        if (referencia == null || referencia.isBlank()) {
            throw new ErrorServiceException("Mercado Pago informó el pago sin una referencia válida.");
        }
        if (pagos.existsByReferenciaAndEliminadoFalse(referencia)) {
            return pagos.findByFactura_IdAndEliminadoFalse(facturaId).stream()
                    .filter(p -> referencia.equals(p.getReferencia())).findFirst().map(PagoService::respuesta)
                    .orElseThrow(() -> new ErrorServiceException("La referencia de pago ya fue utilizada."));
        }
        FormaDePago forma = formas.findByEliminadoFalse().stream()
                .filter(f -> f.getTipoPago() == TipoPago.BILLETERA_VIRTUAL)
                .findFirst().orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay una forma de pago de billetera virtual activa."));
        return registrarEnFactura(factura, forma, monto.doubleValue(), MonedaPago.ARS, referencia,
                "Pago acreditado mediante Checkout Pro de Mercado Pago.");
    }

    public List<PagoResponse> listarPorFactura(String facturaId) {
        if (!facturas.findById(facturaId).filter(f -> !f.isEliminado()).isPresent()) {
            throw new RecursoNoEncontradoException("No se encontró la factura.");
        }
        return pagos.findByFactura_IdAndEliminadoFalse(facturaId).stream().map(PagoService::respuesta).toList();
    }

    public BigDecimal saldoPendienteEnPesos(String facturaId) {
        Factura factura = facturas.findById(facturaId).filter(f -> !f.isEliminado())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la factura."));
        BigDecimal total = BigDecimal.valueOf(factura.getTotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pagado = pagos.findByFactura_IdAndEliminadoFalse(facturaId).stream()
                .map(PagoService::equivalenteEnPesos).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.subtract(pagado).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private PagoResponse registrarEnFactura(Factura factura, FormaDePago forma, double monto,
            MonedaPago moneda, String referencia, String observacion) {
        if (factura.getEstado() != EstadoFactura.PENDIENTE_PAGO) {
            throw new ErrorServiceException("Solo se pueden registrar pagos en facturas pendientes de pago.",
                    "facturaId");
        }
        BigDecimal importe = BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP);
        if (importe.signum() <= 0) throw new ErrorServiceException("El monto debe ser mayor que cero.", "monto");

        Double cotizacionAplicada = null;
        BigDecimal equivalentePesos = importe;
        if (moneda == MonedaPago.USD) {
            CotizacionDolarResponse cotizacion = cotizaciones.buscarCotizacionVigente();
            cotizacionAplicada = cotizacion.valorVenta();
            equivalentePesos = importe.multiply(BigDecimal.valueOf(cotizacionAplicada))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal yaPagado = pagos.findByFactura_IdAndEliminadoFalse(factura.getId()).stream()
                .map(PagoService::equivalenteEnPesos).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFactura = BigDecimal.valueOf(factura.getTotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldo = totalFactura.subtract(yaPagado).max(BigDecimal.ZERO);
        if (equivalentePesos.compareTo(saldo) > 0) {
            throw new ErrorServiceException("El pago supera el saldo pendiente de la factura.", "monto");
        }
        Pago pago = new Pago();
        pago.setFactura(factura);
        pago.setFormaDePago(forma);
        pago.setMonto(importe.doubleValue());
        pago.setMoneda(moneda);
        pago.setCotizacionAplicada(cotizacionAplicada);
        pago.setFechaPago(LocalDate.now());
        pago.setReferencia(referencia);
        pago.setObservacion(observacion == null ? null : observacion.strip());
        Pago guardado = pagos.save(pago);
        if (equivalentePesos.compareTo(saldo) == 0) factura.setEstado(EstadoFactura.PAGADA);
        return respuesta(guardado);
    }

    private Factura buscarFacturaBloqueada(String id) {
        if (id == null || id.isBlank()) throw new ErrorServiceException("Indicá la factura.", "facturaId");
        return facturas.buscarActivaParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la factura."));
    }

    private FormaDePago formaActiva(String id) {
        if (id == null || id.isBlank()) throw new ErrorServiceException("Indicá la forma de pago.", "formaDePagoId");
        return formas.findById(id).filter(f -> !f.isEliminado())
                .orElseThrow(() -> new RecursoNoEncontradoException("La forma de pago no existe o está inactiva."));
    }

    static BigDecimal equivalenteEnPesos(Pago pago) {
        BigDecimal importe = BigDecimal.valueOf(pago.getMonto());
        return pago.getMoneda() == MonedaPago.USD && pago.getCotizacionAplicada() != null
                ? importe.multiply(BigDecimal.valueOf(pago.getCotizacionAplicada())).setScale(2, RoundingMode.HALF_UP)
                : importe.setScale(2, RoundingMode.HALF_UP);
    }

    private static PagoResponse respuesta(Pago pago) {
        return new PagoResponse(pago.getId(), pago.getFactura().getId(), pago.getFormaDePago().getId(),
                pago.getFormaDePago().getTipoPago().name(), pago.getMonto(), pago.getMoneda(),
                pago.getCotizacionAplicada(), equivalenteEnPesos(pago).doubleValue(), pago.getFechaPago(),
                pago.getReferencia(), pago.getObservacion());
    }
}

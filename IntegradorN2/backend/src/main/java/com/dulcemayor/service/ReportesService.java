package com.dulcemayor.service;

import com.dulcemayor.dto.ReporteFiltrosResponse;
import com.dulcemayor.dto.ReporteOpcionResponse;
import com.dulcemayor.dto.ReporteVentaAgrupadaResponse;
import com.dulcemayor.dto.ReporteVentaDetalleResponse;
import com.dulcemayor.dto.ReporteVentasResponse;
import com.dulcemayor.entity.Departamento;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.DepartamentoRepository;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.PreventorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma una única vista de ventas facturadas que comparten el detalle, PDF y Excel de E5-03. */
@Service
@Transactional(readOnly = true)
public class ReportesService {
    private static final List<EstadoPedido> ESTADOS_DE_VENTA = List.of(
            EstadoPedido.FACTURADO, EstadoPedido.EN_REPARTO, EstadoPedido.ENTREGADO);

    private final FacturaRepository facturas;
    private final DepartamentoRepository departamentos;
    private final PreventorRepository preventores;

    public ReportesService(FacturaRepository facturas, DepartamentoRepository departamentos,
            PreventorRepository preventores) {
        this.facturas = facturas;
        this.departamentos = departamentos;
        this.preventores = preventores;
    }

    public ReporteFiltrosResponse listarFiltros() {
        List<ReporteOpcionResponse> opcionesDepartamento = departamentos.findByEliminadoFalseOrderByNombreAsc()
                .stream().map(d -> new ReporteOpcionResponse(d.getId(), d.getNombre())).toList();
        List<ReporteOpcionResponse> opcionesPreventor = preventores.findByEliminadoFalse().stream()
                .sorted(Comparator.comparing(Preventor::getApellido, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Preventor::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(p -> new ReporteOpcionResponse(p.getId(), nombreCompleto(p.getNombre(), p.getApellido())))
                .toList();
        return new ReporteFiltrosResponse(opcionesDepartamento, opcionesPreventor);
    }

    public ReporteVentasResponse buscarVentas(LocalDate desde, LocalDate hasta,
            String departamentoId, String preventorId) {
        validarPeriodo(desde, hasta);
        departamentoId = normalizarId(departamentoId);
        preventorId = normalizarId(preventorId);
        if (departamentoId != null && departamentos.findByIdAndEliminadoFalse(departamentoId).isEmpty()) {
            throw new RecursoNoEncontradoException("No se encontró el departamento seleccionado.");
        }
        if (preventorId != null && preventores.findByIdAndEliminadoFalse(preventorId).isEmpty()) {
            throw new RecursoNoEncontradoException("No se encontró el preventor seleccionado.");
        }

        List<Factura> ventas = facturas.listarVentasParaReporte(desde, hasta, departamentoId, preventorId,
                EstadoFactura.ANULADA, ESTADOS_DE_VENTA);
        List<ReporteVentaDetalleResponse> detalle = ventas.stream().map(this::detalle).toList();
        Map<ClaveResumen, Acumulado> acumulados = new LinkedHashMap<>();
        for (ReporteVentaDetalleResponse venta : detalle) {
            ClaveResumen clave = new ClaveResumen(venta.departamentoId(), venta.departamento(),
                    venta.preventorId(), venta.preventor());
            acumulados.computeIfAbsent(clave, ignorado -> new Acumulado()).agregar(venta.monto());
        }
        List<ReporteVentaAgrupadaResponse> resumen = new ArrayList<>();
        acumulados.forEach((clave, acumulado) -> resumen.add(new ReporteVentaAgrupadaResponse(
                clave.departamentoId(), clave.departamento(), clave.preventorId(), clave.preventor(),
                acumulado.cantidad, acumulado.monto)));
        resumen.sort(Comparator.comparing(ReporteVentaAgrupadaResponse::departamento,
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ReporteVentaAgrupadaResponse::preventor, String.CASE_INSENSITIVE_ORDER));
        BigDecimal total = detalle.stream().map(ReporteVentaDetalleResponse::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        return new ReporteVentasResponse(desde, hasta, departamentoId, preventorId,
                detalle.size(), total, resumen, detalle);
    }

    private ReporteVentaDetalleResponse detalle(Factura factura) {
        Pedido pedido = factura.getPedido();
        var punto = pedido.getPuntoDeVenta();
        var cliente = punto.getMinorista();
        var departamento = punto.getDireccion().getLocalidad().getDepartamento();
        Preventor preventor = pedido.getPreventor();
        return new ReporteVentaDetalleResponse(factura.getId(), factura.getNumeroFactura(), factura.getFechaFactura(),
                pedido.getId(), pedido.getNumeroPedido(), pedido.getFechaHoraCarga(), punto.getNombreComercio(),
                nombreCompleto(cliente.getNombre(), cliente.getApellido()), departamento.getId(),
                departamento.getNombre(), preventor.getId(),
                nombreCompleto(preventor.getNombre(), preventor.getApellido()), pedido.getEstado(),
                BigDecimal.valueOf(factura.getTotal()).setScale(2, RoundingMode.HALF_UP));
    }

    private static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) throw new ErrorServiceException("Indicá la fecha desde y hasta.");
        if (desde.isAfter(hasta)) throw new ErrorServiceException("La fecha desde no puede ser posterior a hasta.", "desde");
    }

    private static String normalizarId(String id) { return id == null || id.isBlank() ? null : id.strip(); }

    private static String nombreCompleto(String nombre, String apellido) {
        return (nombre + " " + apellido).strip();
    }

    private record ClaveResumen(String departamentoId, String departamento, String preventorId, String preventor) {}

    private static class Acumulado {
        private long cantidad;
        private BigDecimal monto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        private void agregar(BigDecimal importe) {
            cantidad++;
            monto = monto.add(importe).setScale(2, RoundingMode.HALF_UP);
        }
    }
}

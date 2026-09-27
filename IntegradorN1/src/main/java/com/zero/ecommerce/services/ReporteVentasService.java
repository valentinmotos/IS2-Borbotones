package com.zero.ecommerce.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.DetalleVentaDTO;
import com.zero.ecommerce.dto.ReporteVentasDTO;
import com.zero.ecommerce.dto.SubtotalFormaPagoDTO;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaClienteRepository;

/**
 * Reporte de ventas (RF28, E5-01): FacturaCliente en PAGADA filtradas por rango de fechaFactura.
 * Solo usa su propio repository (FacturaClienteRepository); navega las asociaciones JPA de la entidad
 * para armar los DTOs sin tocar repositorios ajenos.
 */
@Service
@Transactional(readOnly = true)
public class ReporteVentasService {

    private final FacturaClienteRepository facturaClienteRepository;

    public ReporteVentasService(FacturaClienteRepository facturaClienteRepository) {
        this.facturaClienteRepository = facturaClienteRepository;
    }

    /**
     * Genera el reporte de ventas para el rango [desde, hasta] inclusive.
     * Solo cuentan las FacturaCliente en PAGADA, filtradas por fechaFactura.
     *
     * @throws ErrorServiceException si "desde" es posterior a "hasta".
     */
    public ReporteVentasDTO generar(LocalDate desde, LocalDate hasta) throws ErrorServiceException {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ErrorServiceException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }

        List<FacturaCliente> facturas = facturaClienteRepository
                .findByEstadoAndFechaFacturaBetweenAndEliminadoFalseOrderByFechaFacturaDescNumeroFacturaDesc(
                        EstadoFactura.PAGADA, desde, hasta);

        List<DetalleVentaDTO> detalle = new ArrayList<>();
        for (FacturaCliente factura : facturas) {
            for (DetalleFactura df : factura.getDetalles()) {
                if (df.isEliminado()) {
                    continue;
                }
                Producto producto = df.getProducto();
                SubCategoria subCategoria = producto != null ? producto.getSubCategoria() : null;
                String nombreProducto = producto != null
                        ? producto.getNombre() + " (Talle " + producto.getTalle() + ")"
                        : "—";
                String categoria = (subCategoria != null)
                        ? subCategoria.getCategoria().getNombre() + " / " + subCategoria.getNombre()
                        : "—";
                String identificadorCompra = resolverIdentificador(factura);
                String formaPago = resolverFormaPago(factura.getFormaDePago());

                detalle.add(new DetalleVentaDTO(
                        factura.getFechaFactura(),
                        nombreProducto,
                        categoria,
                        df.getCantidad(),
                        df.getPrecioUnitario(),
                        df.getSubtotal(),
                        identificadorCompra,
                        formaPago,
                        factura.getId(),
                        factura.getOrdenCompra() != null ? factura.getOrdenCompra().getId() : null
                ));
            }
        }

        // Totales globales: una compra = una factura distinta (se cuentan facturas únicas en el detalle)
        long facturasUnicas = facturas.size();
        int unidadesVendidas = detalle.stream().mapToInt(DetalleVentaDTO::cantidad).sum();
        double montoTotal = detalle.stream().mapToDouble(DetalleVentaDTO::subtotal).sum();

        // Subtotales por forma de pago
        Map<String, SubtotalAcumulado> porFormaPago = new LinkedHashMap<>();
        for (FacturaCliente factura : facturas) {
            String fp = resolverFormaPago(factura.getFormaDePago());
            SubtotalAcumulado acum = porFormaPago.computeIfAbsent(fp, k -> new SubtotalAcumulado());
            acum.compras++;
            for (DetalleFactura df : factura.getDetalles()) {
                if (!df.isEliminado()) {
                    acum.unidades += df.getCantidad();
                    acum.monto += df.getSubtotal();
                }
            }
        }
        List<SubtotalFormaPagoDTO> subtotales = new ArrayList<>();
        porFormaPago.forEach((fp, acum) ->
                subtotales.add(new SubtotalFormaPagoDTO(fp, acum.compras, acum.unidades, acum.monto)));

        return new ReporteVentasDTO((int) facturasUnicas, unidadesVendidas, montoTotal, detalle, subtotales);
    }

    // --------------- helpers privados ---------------

    private String resolverIdentificador(FacturaCliente factura) {
        OrdenCompra orden = factura.getOrdenCompra();
        if (orden != null && orden.getIdentificadorCompra() != null) {
            return orden.getIdentificadorCompra();
        }
        // Ventas del seeder sin OrdenCompra: se usa el número de factura
        return "Venta N.º " + factura.getNumeroFactura();
    }

    private String resolverFormaPago(FormaDePago formaDePago) {
        if (formaDePago == null) {
            return "—";
        }
        return formaDePago.getObservacion() != null ? formaDePago.getObservacion()
                : formaDePago.getTipoPago().getDescripcion();
    }

    /** Acumulador interno para sumar compras, unidades y monto por forma de pago. */
    private static class SubtotalAcumulado {
        int compras;
        int unidades;
        double monto;
    }
}

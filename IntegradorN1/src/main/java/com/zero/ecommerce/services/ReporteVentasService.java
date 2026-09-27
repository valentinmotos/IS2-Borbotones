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
        if (desde == null || hasta == null) {
            throw new ErrorServiceException("Las fechas 'desde' y 'hasta' son obligatorias.");
        }
        if (desde.isAfter(hasta)) {
            throw new ErrorServiceException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }

        List<FacturaCliente> facturas = facturaClienteRepository
                .findByEstadoAndFechaFacturaBetweenAndEliminadoFalseOrderByFechaFacturaDescNumeroFacturaDesc(
                        EstadoFactura.PAGADA, desde, hasta);

        List<DetalleVentaDTO> detalle = new ArrayList<>();
        Map<String, SubtotalAcumulado> porFormaPago = new LinkedHashMap<>();
        for (FacturaCliente factura : facturas) {
            FormaDePago formaDePago = factura.getFormaDePago();
            String formaPago = resolverFormaPago(formaDePago);
            SubtotalAcumulado subtotalForma = porFormaPago.computeIfAbsent(claveFormaPago(formaDePago),
                    clave -> new SubtotalAcumulado(formaPago));
            subtotalForma.compras++;

            for (DetalleFactura df : factura.getDetalles()) {
                if (df.isEliminado()) {
                    continue;
                }
                Producto producto = df.getProducto();
                SubCategoria subCategoria = producto != null ? producto.getSubCategoria() : null;
                String nombreProducto = resolverProducto(producto);
                String categoria = resolverCategoria(subCategoria);
                String identificadorCompra = resolverIdentificador(factura);

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
                subtotalForma.unidades += df.getCantidad();
                subtotalForma.monto += df.getSubtotal();
            }
        }

        // Totales globales: una compra = una factura pagada del rango, aunque tenga más de un renglón.
        int cantidadCompras = facturas.size();
        int unidadesVendidas = detalle.stream().mapToInt(DetalleVentaDTO::cantidad).sum();
        double montoTotal = detalle.stream().mapToDouble(DetalleVentaDTO::subtotal).sum();

        List<SubtotalFormaPagoDTO> subtotales = porFormaPago.values().stream()
                .map(acum -> new SubtotalFormaPagoDTO(
                        acum.formaPago, acum.compras, acum.unidades, acum.monto))
                .toList();

        return new ReporteVentasDTO(cantidadCompras, unidadesVendidas, montoTotal,
                List.copyOf(detalle), subtotales);
    }

    // --------------- helpers privados ---------------

    private String resolverIdentificador(FacturaCliente factura) {
        OrdenCompra orden = factura.getOrdenCompra();
        if (orden != null && orden.getIdentificadorCompra() != null && !orden.getIdentificadorCompra().isBlank()) {
            return orden.getIdentificadorCompra();
        }
        // Ventas del seeder sin OrdenCompra: se usa el número de factura
        return "Venta N.º " + factura.getNumeroFactura();
    }

    private String resolverFormaPago(FormaDePago formaDePago) {
        if (formaDePago == null) {
            return "—";
        }
        if (formaDePago.getObservacion() != null && !formaDePago.getObservacion().isBlank()) {
            return formaDePago.getObservacion();
        }
        return formaDePago.getTipoPago() == null ? "—" : formaDePago.getTipoPago().getDescripcion();
    }

    /** La clave evita mezclar dos formas distintas que casualmente tengan la misma observación. */
    private String claveFormaPago(FormaDePago formaDePago) {
        if (formaDePago == null) {
            return "sin-forma-de-pago";
        }
        if (formaDePago.getId() != null && !formaDePago.getId().isBlank()) {
            return "id:" + formaDePago.getId();
        }
        return "valor:" + formaDePago.getTipoPago() + ":" + formaDePago.getObservacion();
    }

    private String resolverProducto(Producto producto) {
        if (producto == null) {
            return "—";
        }
        if (producto.getTalle() == null || producto.getTalle().isBlank()) {
            return producto.getNombre();
        }
        return producto.getNombre() + " (Talle " + producto.getTalle() + ")";
    }

    private String resolverCategoria(SubCategoria subCategoria) {
        if (subCategoria == null) {
            return "—";
        }
        if (subCategoria.getCategoria() == null) {
            return subCategoria.getNombre();
        }
        return subCategoria.getCategoria().getNombre() + " / " + subCategoria.getNombre();
    }

    /** Acumulador interno para sumar compras, unidades y monto por forma de pago. */
    private static class SubtotalAcumulado {
        final String formaPago;
        int compras;
        int unidades;
        double monto;

        SubtotalAcumulado(String formaPago) {
            this.formaPago = formaPago;
        }
    }
}

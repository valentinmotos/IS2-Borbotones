package com.zero.ecommerce.services;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.DashboardDTO;
import com.zero.ecommerce.dto.ProductoMasVendidoDTO;
import com.zero.ecommerce.dto.ProductoPrecioVencidoDTO;
import com.zero.ecommerce.dto.ReporteStockDTO;
import com.zero.ecommerce.dto.VentaMensualDTO;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaClienteRepository;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final DateTimeFormatter MES_FORMATTER = DateTimeFormatter.ofPattern("MMM");

    private final FacturaClienteRepository facturaClienteRepository;
    private final OrdenCompraService ordenCompraService;
    private final ClienteService clienteService;
    private final ReporteStockService reporteStockService;
    private final VigenciaPrecioService vigenciaPrecioService;

    public DashboardService(FacturaClienteRepository facturaClienteRepository, OrdenCompraService ordenCompraService,
            ClienteService clienteService, ReporteStockService reporteStockService,
            VigenciaPrecioService vigenciaPrecioService) {
        this.facturaClienteRepository = facturaClienteRepository;
        this.ordenCompraService = ordenCompraService;
        this.clienteService = clienteService;
        this.reporteStockService = reporteStockService;
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    public DashboardDTO generar() throws ErrorServiceException {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioMesSiguiente = inicioMes.plusMonths(1);
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1);

        List<FacturaCliente> ventas = facturaClienteRepository.findByEliminadoFalseOrderByFechaFacturaAsc().stream()
                .filter(factura -> factura.getEstado() == EstadoFactura.PAGADA)
                .filter(factura -> factura.getFechaFactura() != null && !factura.getFechaFactura().isAfter(hoy))
                .toList();

        double ventasMesMonto = ventas.stream()
                .filter(f -> estaEnRango(f, inicioMes, inicioMesSiguiente))
                .mapToDouble(FacturaCliente::getTotalPagado)
                .sum();
        long ventasMesCantidad = ventas.stream()
                .filter(f -> estaEnRango(f, inicioMes, inicioMesSiguiente))
                .count();

        double ventasMesAnteriorMonto = ventas.stream()
                .filter(f -> estaEnRango(f, inicioMesAnterior, inicioMes))
                .mapToDouble(FacturaCliente::getTotalPagado)
                .sum();

        double variacionMesAnterior = calcularVariacion(ventasMesMonto, ventasMesAnteriorMonto);

        long pedidosPendientesPago = ordenCompraService.listarPedidoActivo().stream()
                .filter(orden -> orden.getEstadoOrdenCompra() == EstadoOrdenCompra.PENDIENTE_PAGO)
                .count();
        long pedidosPendientesEnvio = ordenCompraService.listarPedidoActivo().stream()
                .filter(orden -> orden.getEstadoOrdenCompra() == EstadoOrdenCompra.PENDIENTE_ENVIO)
                .count();

        ReporteStockDTO reporteStock = reporteStockService.generar();
        List<ProductoPrecioVencidoDTO> preciosVencidos = vigenciaPrecioService.listarProductosConPrecioVencido();
        long clientesRegistrados = clienteService.listarClienteActivo().size();
        List<VentaMensualDTO> ventasUltimosSeisMeses = calcularVentasUltimosSeisMeses(ventas, hoy);
        List<ProductoMasVendidoDTO> productosMasVendidos = calcularTopProductos(ventas);

        return new DashboardDTO(ventasMesCantidad, redondear(ventasMesMonto), variacionMesAnterior,
                pedidosPendientesPago, pedidosPendientesEnvio, reporteStock.cantidadMalo(), preciosVencidos.size(),
                clientesRegistrados, ventasUltimosSeisMeses, productosMasVendidos);
    }

    private double calcularVariacion(double actual, double anterior) {
        if (anterior <= 0) {
            return actual <= 0 ? 0.0 : 100.0;
        }
        return ((actual - anterior) / anterior) * 100.0;
    }

    private boolean estaEnRango(FacturaCliente factura, LocalDate desdeInclusive, LocalDate hastaExclusive) {
        LocalDate fecha = factura.getFechaFactura();
        return fecha != null && !fecha.isBefore(desdeInclusive) && fecha.isBefore(hastaExclusive);
    }

    private List<VentaMensualDTO> calcularVentasUltimosSeisMeses(List<FacturaCliente> ventas, LocalDate hoy) {
        List<VentaMensualDTO> resultado = new ArrayList<>();
        YearMonth mesActual = YearMonth.from(hoy);
        for (int i = 5; i >= 0; i--) {
            YearMonth mes = mesActual.minusMonths(i);
            LocalDate inicio = mes.atDay(1);
            LocalDate fin = mes.plusMonths(1).atDay(1).minusDays(1);

            double monto = ventas.stream()
                    .filter(f -> f.getFechaFactura() != null && !f.getFechaFactura().isBefore(inicio)
                            && !f.getFechaFactura().isAfter(fin))
                    .mapToDouble(FacturaCliente::getTotalPagado)
                    .sum();
            long cantidad = ventas.stream()
                    .filter(f -> f.getFechaFactura() != null && !f.getFechaFactura().isBefore(inicio)
                            && !f.getFechaFactura().isAfter(fin))
                    .count();

            resultado.add(new VentaMensualDTO(mes.toString(), mes.format(MES_FORMATTER), cantidad,
                    redondear(monto)));
        }
        return resultado;
    }

    private List<ProductoMasVendidoDTO> calcularTopProductos(List<FacturaCliente> ventas) {
        Map<String, ResumenProducto> resumen = new LinkedHashMap<>();
        for (FacturaCliente factura : ventas) {
            if (factura.getDetalles() == null) {
                continue;
            }
            for (DetalleFactura detalle : factura.getDetalles()) {
                if (detalle == null || detalle.getProducto() == null || detalle.isEliminado()) {
                    continue;
                }
                Producto producto = detalle.getProducto();
                ResumenProducto actual = resumen.computeIfAbsent(producto.getId(), id -> new ResumenProducto(producto));
                actual.cantidad += detalle.getCantidad();
                actual.monto += detalle.getSubtotal();
            }
        }

        return resumen.values().stream()
                .sorted(Comparator.<ResumenProducto>comparingLong(r -> r.cantidad).reversed()
                        .thenComparing(Comparator.comparingDouble((ResumenProducto r) -> r.monto).reversed())
                        .thenComparing(r -> r.producto.getNombre(), String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .map(resumenProducto -> new ProductoMasVendidoDTO(
                        resumenProducto.producto.getId(),
                        resumenProducto.producto.getNombre(),
                        resumenProducto.producto.getTalle(),
                        resumenProducto.cantidad,
                        redondear(resumenProducto.monto)))
                .toList();
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static final class ResumenProducto {
        private final Producto producto;
        private long cantidad;
        private double monto;

        private ResumenProducto(Producto producto) {
            this.producto = producto;
        }
    }
}

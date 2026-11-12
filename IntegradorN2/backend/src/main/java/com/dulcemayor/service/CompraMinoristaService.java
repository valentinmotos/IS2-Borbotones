package com.dulcemayor.service;

import com.dulcemayor.dto.ArchivoDescargable;
import com.dulcemayor.dto.CompraMinoristaDetalleResponse;
import com.dulcemayor.dto.CompraMinoristaResponse;
import com.dulcemayor.dto.mapper.CompraMinoristaMapper;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.HistorialEstadoPedidoRepository;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.repository.PedidoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4-08: compras y facturas del minorista autenticado (historial, línea de tiempo y descarga).
 *
 * <p>El minorista sale siempre del token ({@code idUsuario}) y todas las consultas filtran por
 * {@code pedido.puntoDeVenta.minorista}. Un pedido o una factura de otro minorista responde 404
 * (igual que el checkout de E4-05), así no se revela que existe.
 *
 * <p>NOTA (E4-08): el UML pone {@code listarFacturaPorMinorista(idMinorista)} en el service de
 * Factura. Para no tocar {@code FacturacionService} (gestión, E4-04) la lectura del minorista se
 * concentra acá y recibe el id del <em>usuario</em> del token, no el del minorista, para que nadie
 * pueda pedir las compras de otro pasando un id.
 */
@Service
@Transactional(readOnly = true)
public class CompraMinoristaService {
    private static final String PERFIL_NO_ENCONTRADO = "No se encontró tu perfil de minorista.";
    private static final String PEDIDO_NO_ENCONTRADO = "No se encontró el pedido.";
    private static final String FACTURA_NO_ENCONTRADA = "No se encontró la factura.";

    private final MinoristaRepository minoristas;
    private final PedidoRepository pedidos;
    private final FacturaRepository facturas;
    private final HistorialEstadoPedidoRepository historial;
    private final PagoService pagos;
    private final FacturaPdfService pdf;
    private final CompraMinoristaMapper mapper;

    public CompraMinoristaService(MinoristaRepository minoristas, PedidoRepository pedidos,
            FacturaRepository facturas, HistorialEstadoPedidoRepository historial, PagoService pagos,
            FacturaPdfService pdf, CompraMinoristaMapper mapper) {
        this.minoristas = minoristas;
        this.pedidos = pedidos;
        this.facturas = facturas;
        this.historial = historial;
        this.pagos = pagos;
        this.pdf = pdf;
        this.mapper = mapper;
    }

    /** Todos los pedidos de los puntos de venta del minorista, del más nuevo al más viejo. */
    public List<CompraMinoristaResponse> listarPedidoPorMinorista(String idUsuario) {
        String minoristaId = minoristaDelUsuario(idUsuario).getId();
        Map<String, Factura> facturaPorPedido = facturas
                .findByPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse(minoristaId).stream()
                .collect(Collectors.toMap(f -> f.getPedido().getId(), Function.identity(), (a, b) -> a));
        return pedidos.findByPuntoDeVenta_Minorista_IdAndEliminadoFalseOrderByFechaHoraCargaDescIdDesc(minoristaId)
                .stream()
                .map(pedido -> compra(pedido, facturaPorPedido.get(pedido.getId())))
                .toList();
    }

    /** Detalle de un pedido propio con sus líneas y la línea de tiempo. 404 si no es del minorista. */
    public CompraMinoristaDetalleResponse buscarPedidoDelMinorista(String idUsuario, String idPedido) {
        String minoristaId = minoristaDelUsuario(idUsuario).getId();
        Pedido pedido = pedidos.findByIdAndPuntoDeVenta_Minorista_IdAndEliminadoFalse(idPedido, minoristaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(PEDIDO_NO_ENCONTRADO));
        Factura factura = facturas.findByPedido_IdAndEliminadoFalse(pedido.getId()).orElse(null);
        return new CompraMinoristaDetalleResponse(compra(pedido, factura), pedido.getCodigoPromocion(),
                mapper.aLineas(pedido.getDetalles()),
                mapper.aEventos(historial.findByPedido_IdAndEliminadoFalseOrderByFechaHoraAscIdAsc(pedido.getId())));
    }

    /** PDF de una factura propia, generado en memoria por E4-04. 404 si no es del minorista. */
    public ArchivoDescargable descargarFacturaDelMinorista(String idUsuario, String idFactura) {
        String minoristaId = minoristaDelUsuario(idUsuario).getId();
        facturas.findByIdAndPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse(idFactura, minoristaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(FACTURA_NO_ENCONTRADA));
        return new ArchivoDescargable(pdf.nombreArchivo(idFactura), pdf.generarFacturaPdf(idFactura));
    }

    private CompraMinoristaResponse compra(Pedido pedido, Factura factura) {
        BigDecimal saldo = null;
        if (factura != null) {
            saldo = factura.getEstado() == EstadoFactura.PENDIENTE_PAGO
                    ? pagos.saldoPendienteEnPesos(factura.getId())
                    : BigDecimal.ZERO.setScale(2);
        }
        boolean puedePagar = saldo != null && saldo.signum() > 0;
        return mapper.aCompra(pedido, factura, saldo, puedePagar);
    }

    private Minorista minoristaDelUsuario(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new RecursoNoEncontradoException(PERFIL_NO_ENCONTRADO);
        }
        return minoristas.findByUsuario_IdAndEliminadoFalse(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(PERFIL_NO_ENCONTRADO));
    }
}

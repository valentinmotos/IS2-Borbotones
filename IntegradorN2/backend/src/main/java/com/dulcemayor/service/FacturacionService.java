package com.dulcemayor.service;

import com.dulcemayor.dto.FacturaResponse;
import com.dulcemayor.dto.FaltanteStockDTO;
import com.dulcemayor.dto.ResultadoFacturacionResponse;
import com.dulcemayor.dto.mapper.FacturaMapper;
import com.dulcemayor.entity.DetalleFactura;
import com.dulcemayor.entity.DetallePedido;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.PedidoRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * E4-04: facturación automática de pedidos.
 *
 * Procesar un pedido (CARGADO o SIN_STOCK) verifica el stock de todas sus líneas:
 * <ul>
 *   <li>Si alcanza: descuenta el stock de cada producto, emite la factura (número secuencial, estado
 *       PENDIENTE_PAGO, detalle copiado del pedido), pasa el pedido a FACTURADO y publica
 *       {@link FacturaEmitidaEvent}. Todo en una transacción: si un descuento falla, no queda nada a medias.</li>
 *   <li>Si no alcanza: el pedido pasa a SIN_STOCK (si ya estaba, no se repite el registro del historial)
 *       y se devuelven los faltantes. No se toca el stock.</li>
 * </ul>
 * El PDF se arma a pedido con {@code FacturaPdfService} (no se guarda).
 */
@Service
@Transactional(readOnly = true)
public class FacturacionService {
    private static final String PEDIDO_NO_ENCONTRADO = "No se encontró el pedido.";
    private static final int MAX_OBSERVACION = 500;

    private final PedidoRepository pedidos;
    private final FacturaRepository facturas;
    private final StockService stock;
    private final PedidoService estados;
    private final FacturaMapper mapper;
    private final ApplicationEventPublisher eventos;
    private final TransactionTemplate transacciones;

    public FacturacionService(PedidoRepository pedidos, FacturaRepository facturas, StockService stock,
            PedidoService estados, FacturaMapper mapper, ApplicationEventPublisher eventos,
            TransactionTemplate transacciones) {
        this.pedidos = pedidos;
        this.facturas = facturas;
        this.stock = stock;
        this.estados = estados;
        this.mapper = mapper;
        this.eventos = eventos;
        this.transacciones = transacciones;
    }

    /** Número de factura como se imprime: 8 dígitos con ceros a la izquierda. */
    public static String numeroVisible(long numeroFactura) {
        return String.format("%08d", numeroFactura);
    }

    /**
     * Procesa y, si hay stock, factura el pedido.
     *
     * NOTA (E4-04): el contrato del kickoff es {@code facturarPedido(idPedido)}; se agregó {@code idUsuario}
     * porque E4-01 exige el usuario responsable en cada registro de HistorialEstadoPedido. Quien lo consuma
     * (E4-06 panel de almacén, E4-07, E6-03) pasa el id del usuario logueado, que el controller saca del
     * token ({@code @AuthenticationPrincipal UsuarioAutenticado}), nunca del cuerpo de la solicitud.
     *
     * @return FACTURADO con la factura, o SIN_STOCK con los faltantes
     * @throws RecursoNoEncontradoException si el pedido no existe (404)
     * @throws ErrorServiceException si el pedido no está CARGADO ni SIN_STOCK, o el usuario no es válido (400)
     * @throws DuplicadoException si otra facturación simultánea tomó el mismo número (409, se puede reintentar)
     */
    @Transactional
    public ResultadoFacturacionResponse facturarPedido(String idPedido, String idUsuario) {
        // El bloqueo del pedido evita que dos operadores lo facturen dos veces al mismo tiempo.
        Pedido pedido = pedidos.buscarActivoParaActualizar(idPedido)
                .orElseThrow(() -> new RecursoNoEncontradoException(PEDIDO_NO_ENCONTRADO));
        validar(pedido);

        List<FaltanteStockDTO> faltantes = buscarFaltantes(pedido);
        if (!faltantes.isEmpty()) {
            if (pedido.getEstado() == EstadoPedido.CARGADO) {
                estados.avanzar(pedido.getId(), EventoPedido.MARCAR_SIN_STOCK, idUsuario,
                        observacionFaltantes(faltantes), null);
            }
            return new ResultadoFacturacionResponse(pedido.getId(), pedido.getNumeroPedido(), EstadoPedido.SIN_STOCK,
                    null, faltantes, "No hay stock suficiente para facturar el pedido.");
        }

        Factura factura = crearFactura(pedido);
        estados.avanzar(pedido.getId(), EventoPedido.FACTURAR, idUsuario,
                "Factura N° " + numeroVisible(factura.getNumeroFactura()) + " emitida.", null);
        eventos.publishEvent(new FacturaEmitidaEvent(factura.getId(), pedido.getId()));
        return new ResultadoFacturacionResponse(pedido.getId(), pedido.getNumeroPedido(), EstadoPedido.FACTURADO,
                mapper.aRespuesta(factura), List.of(),
                "Pedido facturado con la factura N° " + numeroVisible(factura.getNumeroFactura()) + ".");
    }

    /**
     * Procesa todos los pedidos CARGADO y SIN_STOCK en el orden en que se cargaron. Cada pedido va en su
     * propia transacción: si uno falla, el resto se procesa igual y el motivo vuelve en {@code mensaje}.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<ResultadoFacturacionResponse> procesarPedidosPendientes(String idUsuario) {
        List<String> ids = transacciones.execute(estado -> pedidos
                .findByEstadoInAndEliminadoFalseOrderByFechaHoraCargaAscIdAsc(
                        List.of(EstadoPedido.CARGADO, EstadoPedido.SIN_STOCK))
                .stream().map(Pedido::getId).toList());
        List<ResultadoFacturacionResponse> resultados = new ArrayList<>();
        for (String id : ids == null ? List.<String>of() : ids) {
            try {
                resultados.add(transacciones.execute(estado -> facturarPedido(id, idUsuario)));
            } catch (ErrorServiceException ex) {
                resultados.add(ResultadoFacturacionResponse.error(id, ex.getMessage()));
            }
        }
        return resultados;
    }

    public FacturaResponse buscarFactura(String id) {
        return mapper.aRespuesta(facturas.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la factura.")));
    }

    public FacturaResponse buscarFacturaPorPedido(String idPedido) {
        pedidos.findByIdAndEliminadoFalse(idPedido)
                .orElseThrow(() -> new RecursoNoEncontradoException(PEDIDO_NO_ENCONTRADO));
        return mapper.aRespuesta(facturas.findByPedido_IdAndEliminadoFalse(idPedido)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pedido todavía no tiene factura.")));
    }

    public List<FacturaResponse> listarFactura() {
        return mapper.aRespuestas(facturas.findByEliminadoFalseOrderByNumeroFacturaDesc());
    }

    public List<FacturaResponse> listarFacturaPorEstado(EstadoFactura estado) {
        return mapper.aRespuestas(facturas.findByEstadoAndEliminadoFalseOrderByNumeroFacturaDesc(estado));
    }

    /** Solo se procesan pedidos CARGADO o SIN_STOCK que todavía no tengan factura. */
    void validar(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.CARGADO && pedido.getEstado() != EstadoPedido.SIN_STOCK) {
            throw new ErrorServiceException("El pedido está en " + pedido.getEstado()
                    + " y no se puede facturar.", "estado");
        }
        if (facturas.findByPedido_IdAndEliminadoFalse(pedido.getId()).isPresent()) {
            throw new ErrorServiceException("El pedido ya tiene una factura emitida.", "estado");
        }
        if (pedido.getDetalles().isEmpty()) {
            throw new ErrorServiceException("El pedido no tiene productos para facturar.", "detalles");
        }
    }

    /**
     * Crea la factura con el detalle del pedido y descuenta el stock. Los descuentos se hacen en orden de id
     * de producto para que dos facturaciones simultáneas tomen los bloqueos en el mismo orden (sin deadlock).
     *
     * NOTA (E4-04): el número es el último emitido + 1. La columna {@code numero_factura} es única, así que
     * dos facturaciones simultáneas nunca comparten número: la segunda falla con 409 y se reintenta. Si el
     * volumen de facturación concurrente creciera, reemplazar por una secuencia de la base sin cambiar la firma.
     */
    private Factura crearFactura(Pedido pedido) {
        long numero = facturas.ultimoNumeroFactura() + 1;
        Factura factura = new Factura();
        factura.setNumeroFactura(numero);
        factura.setFechaFactura(LocalDate.now());
        factura.setTotal(pedido.getTotal());
        factura.setEstado(EstadoFactura.PENDIENTE_PAGO);
        factura.setPedido(pedido);

        String observacion = "Factura N° " + numeroVisible(numero) + " · pedido N° " + pedido.getNumeroPedido();
        cantidadesPorProducto(pedido).forEach((productoId, cantidad) ->
                stock.descontar(productoId, cantidad, observacion));

        for (DetallePedido linea : pedido.getDetalles()) {
            DetalleFactura detalle = new DetalleFactura();
            detalle.setProducto(linea.getProducto());
            detalle.setCantidad(linea.getCantidad());
            detalle.setPrecioUnitario(linea.getPrecioUnitario());
            detalle.setSubtotal(linea.getSubtotal());
            factura.agregarDetalle(detalle);
        }
        try {
            return facturas.saveAndFlush(factura);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicadoException(
                    "Otra factura se emitió al mismo tiempo. Volvé a procesar el pedido.", "numeroFactura");
        }
    }

    private List<FaltanteStockDTO> buscarFaltantes(Pedido pedido) {
        Map<String, Producto> productos = new TreeMap<>();
        pedido.getDetalles().forEach(linea -> productos.put(linea.getProducto().getId(), linea.getProducto()));
        List<FaltanteStockDTO> faltantes = new ArrayList<>();
        cantidadesPorProducto(pedido).forEach((productoId, cantidad) -> {
            int disponible = disponible(productoId);
            if (disponible < cantidad) {
                faltantes.add(new FaltanteStockDTO(productoId, productos.get(productoId).getNombre(),
                        cantidad, disponible));
            }
        });
        return faltantes;
    }

    /** Suma las líneas del mismo producto; el TreeMap fija el orden de los bloqueos. */
    private Map<String, Integer> cantidadesPorProducto(Pedido pedido) {
        Map<String, Integer> cantidades = new TreeMap<>();
        pedido.getDetalles().forEach(linea ->
                cantidades.merge(linea.getProducto().getId(), linea.getCantidad(), Integer::sum));
        return cantidades;
    }

    /** Un producto dado de baja después de cargar el pedido no tiene stock disponible. */
    private int disponible(String productoId) {
        try {
            return stock.buscarStockPorProducto(productoId).cantidadActual();
        } catch (RecursoNoEncontradoException ex) {
            return 0;
        }
    }

    private String observacionFaltantes(List<FaltanteStockDTO> faltantes) {
        StringBuilder texto = new StringBuilder("Stock insuficiente:");
        for (FaltanteStockDTO faltante : faltantes) {
            texto.append(' ').append(faltante.producto()).append(" (pide ").append(faltante.cantidadPedida())
                    .append(", hay ").append(faltante.stockDisponible()).append(");");
        }
        return texto.length() <= MAX_OBSERVACION ? texto.toString()
                : texto.substring(0, MAX_OBSERVACION - 3) + "...";
    }
}

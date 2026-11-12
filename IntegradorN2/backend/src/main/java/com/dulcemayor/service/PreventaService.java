package com.dulcemayor.service;

import com.dulcemayor.dto.PedidoPreventaRequest;
import com.dulcemayor.dto.PedidoPreventaResponse;
import com.dulcemayor.entity.DetallePedido;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.PedidoRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.PuntoDeVentaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PreventaService {
    private final PreventorService preventores;
    private final PuntoDeVentaRepository puntos;
    private final ProductoRepository productos;
    private final PedidoRepository pedidos;
    private final PedidoService estados;

    public PreventaService(PreventorService preventores, PuntoDeVentaRepository puntos,
            ProductoRepository productos, PedidoRepository pedidos, PedidoService estados) {
        this.preventores = preventores;
        this.puntos = puntos;
        this.productos = productos;
        this.pedidos = pedidos;
        this.estados = estados;
    }

    public List<PedidoPreventaResponse> listarPedidos(String usuarioId) {
        var preventor = preventores.buscarPreventorPorUsuario(usuarioId);
        return pedidos.findByPreventor_IdAndEliminadoFalseOrderByFechaHoraCargaDescIdDesc(preventor.getId())
                .stream().map(PedidoPreventaResponse::desde).toList();
    }

    @Transactional
    public PedidoPreventaResponse cargarPedido(PedidoPreventaRequest request, String usuarioId) {
        var preventor = preventores.bloquearPerfilParaCarga(usuarioId);
        String claveCarga = request.claveCarga() == null ? null : request.claveCarga().toString();
        if (claveCarga != null) {
            var existente = pedidos.findByPreventor_IdAndClaveCarga(preventor.getId(), claveCarga);
            if (existente.isPresent()) {
                return PedidoPreventaResponse.desde(existente.get());
            }
        }
        var punto = puntos.findByIdAndPreventor_IdAndEliminadoFalseAndMinorista_EliminadoFalse(
                request.puntoDeVentaId(), preventor.getId())
                .orElseThrow(() -> new ErrorServiceException("Seleccioná un punto de venta activo asignado a vos.",
                        "puntoDeVentaId"));
        // El mismo orden de bloqueo para todos los pedidos evita ciclos si coinciden sus productos.
        var cantidades = new TreeMap<String, Integer>();
        for (var linea : request.lineas()) {
            if (cantidades.putIfAbsent(linea.productoId(), linea.cantidad()) != null) {
                throw new ErrorServiceException("Ingresá una sola cantidad por producto.", "lineas");
            }
        }

        var pedido = new Pedido();
        pedido.setPreventor(preventor);
        pedido.setPuntoDeVenta(punto);
        pedido.setClaveCarga(claveCarga);
        String codigo = request.codigoPromocion();
        pedido.setCodigoPromocion(codigo == null || codigo.isBlank() ? null : codigo.strip());

        LocalDate hoy = LocalDate.now();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var linea : cantidades.entrySet()) {
            String productoId = linea.getKey();
            int cantidad = linea.getValue();
            var producto = productos.buscarActivoParaActualizar(productoId)
                    .orElseThrow(() -> new ErrorServiceException("Uno de los productos ya no está activo.", "lineas"));
            var disponible = productos.buscarCatalogo(productoId, "", "", hoy).stream().findFirst()
                    .orElseThrow(() -> new ErrorServiceException(
                            producto.getNombre() + " ya no tiene stock o precio vigente.", "lineas"));
            if (cantidad > disponible.stock()) {
                throw new ErrorServiceException("Quedan " + disponible.stock() + " bultos de "
                        + producto.getNombre() + ".", "lineas");
            }
            BigDecimal importe = BigDecimal.valueOf(disponible.precio()).multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);
            var detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(disponible.precio());
            detalle.setSubtotal(importe.doubleValue());
            pedido.agregarDetalle(detalle);
            subtotal = subtotal.add(importe);
        }
        pedido.setNumeroPedido(nuevoNumero());
        pedido.setSubtotal(subtotal.doubleValue());
        pedido.setTotal(subtotal.doubleValue());
        return PedidoPreventaResponse.desde(estados.registrarCarga(pedido, usuarioId, null));
    }

    private long nuevoNumero() {
        long numero;
        do {
            numero = ThreadLocalRandom.current().nextLong(100_000_000_000_000L, 1_000_000_000_000_000L);
        } while (pedidos.existsByNumeroPedido(numero));
        return numero;
    }
}
